
/*
 * $Id: EchoServlet.java,v 1.5 2003/06/22 12:32:15 fukuda Exp $
 */
package org.mobicents.servlet.sip.example;

import java.util.*;
import java.io.IOException;

import javax.servlet.sip.SipServlet;	
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.ServletException;
import javax.servlet.sip.URI;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.Address;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipSessionsUtil;

/**
 */
public class Myapp extends SipServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private Map<String, String> RegistrarDB;
	static private SipFactory factory;
	private static final String dominioPretendido = "acme.pt";

	private Map<String, String> userStatusMap = new HashMap<>();
	
	public Myapp() {
		super();
		RegistrarDB = new HashMap<String,String>();
	}
	
	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	/**
        * Acts as a registrar and deregistar and location service for REGISTER messages
		* It chooses which operation (REGISTER or DEREGISTER) is in the SIP message received
        * @param  request The SIP message received by the AS 
    	*/
	protected void doRegister(SipServletRequest request) throws ServletException,
			IOException {
		
		String to = request.getHeader("To"); 
    	String aor = getSIPuri(request.getHeader("To")); 

		int expires = Integer.parseInt(getPortExpires(request.getHeader("Contact")));

		if (expires != 0) { 
 			doRegistration(request, to, aor);

		} else { 
			doDeregistration(request, aor); 
		}
	}

	/**
        * This is the function that actually manages the REGISTER operation
        * @param request The SIP message received by the AS, 
		* @param to From the SIP message received, 
		* @param aor From the SIP message received
    	*/
	private void doRegistration(SipServletRequest request, String to, String aor) throws ServletException, IOException {
    	SipServletResponse respons

		String domain = aor.substring(aor.indexOf("@") + 1, aor.length()); 
        String contact = getSIPuriPort(request.getHeader("Contact")); 

			if ("acme.pt".equals(domain)) { 
				RegistrarDB.put(aor, contact); 
				setStatus(aor, "AVAILABLE"); 
				response = request.createResponse(200);
            	response.send(); 
				
			} else { 
				response = request.createResponse(401); 
            	response.send(); 
			}

		log("----------------------------------------------REGISTER (myapp):----------------------------------------------");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    			while (it.hasNext()) {
        			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        			System.out.println(pairs.getKey() + " = " + pairs.getValue());
    			}
		log("----------------------------------------------REGISTER (myapp):----------------------------------------------");
	}

	/**
        * This is the function that actually manages the DEREGISTER operation
        * @param request The SIP message received by the AS, 
		* @param aor From the SIP message received
    	*/
	private void doDeregistration(SipServletRequest request, String aor) throws ServletException, IOException {
    	SipServletResponse response; 

		if (RegistrarDB.containsKey(aor)) { 
			RegistrarDB.remove(aor); 
			userStatusMap.remove(aor);
			response = request.createResponse(200);
        	response.send(); 
		
		} else {
			response = request.createResponse(403); 
        	response.send(); 
		}

		// Some logs to show the content of the Registrar database.
		log("----------------------------------------------DEREGISTER (myapp):----------------------------------------------");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    			while (it.hasNext()) {
        			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        			System.out.println(pairs.getKey() + " = " + pairs.getValue());
    			}
		log("----------------------------------------------DEREGISTER (myapp):----------------------------------------------");
	}

	/**
        * Sends SIP replies to INVITE messages
        * - 300 if registred
        * - 404 if not registred
        * @param  request The SIP message received by the AS 
		* 
        */




	protected void doInvite(SipServletRequest request)
            throws ServletException, IOException {
		
		String fromAor = getSIPuri(request.getHeader("From")); 
		String toAor = getSIPuri(request.getHeader("To")); 
		String domain = toAor.substring(toAor.indexOf("@")+1, toAor.length());
		
		// Some logs to show the content of the Registrar database.
		log("----------------------------------------------INVITE (myapp):----------------------------------------------");
			Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
    			while (it.hasNext()) {
        			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
        			System.out.println(pairs.getKey() + " = " + pairs.getValue());
    			}
		log("----------------------------------------------INVITE (myapp):----------------------------------------------");
		
		if (domain.equals("acme.pt")) { 
			if (!RegistrarDB.containsKey(fromAor)) { 
				SipServletResponse response = request.createResponse(900); 
				log("ERRO MENGA");
				response.send();
	    	} else if (toAor.contains("chat")) { 
					Proxy proxy = request.getProxy();
                	proxy.setRecordRoute(true); 
                	proxy.setSupervised(false);
                	URI toContact = factory.createURI("sip:chat@127.0.0.1:5070");
                	proxy.proxyTo(toContact);
			} else if (!RegistrarDB.containsKey(toAor)) { 
				SipServletResponse response = request.createResponse(404);
				response.send();
	    	} else {
				if (!getStatus(toAor).equals("AVAILABLE")) { 
                	SipServletResponse response = request.createResponse(486);
                	response.send();
            	} else {
                	Proxy proxy = request.getProxy();
                	proxy.setRecordRoute(true);
                	proxy.setSupervised(true);
                	URI toContact = factory.createURI(RegistrarDB.get(toAor));
                	proxy.proxyTo(toContact);
           		}
			}		

		} else {
			SipServletResponse response = request.createResponse(901);
        	response.send();
		}

	}

	/**
        * This is the function that manages the ACK operation
        * @param fromAor From the SIP message received, 
		* @param toAor From the SIP message received
    	*/
	protected void doAck(SipServletRequest request) throws ServletException, IOException {
    	String fromAor = getSIPuri(request.getHeader("From"));
    	String toAor = getSIPuri(request.getHeader("To"));

		if (toAor.contains("chat")) { 
			setStatus(fromAor, "IN CONFERENCE");
		} else {  
    		setStatus(fromAor, "BUSY");
			setStatus(toAor, "BUSY");
		}
	}

	/**
        * This is the function that manages the BYE operation
        * @param fromAor From the SIP message received, 
		* @param toAor From the SIP message received
    	*/
	protected void doBye(SipServletRequest request) throws ServletException, IOException {
    	String fromAor = getSIPuri(request.getHeader("From"));
    	String toAor = getSIPuri(request.getHeader("To"));

		if (toAor.contains("chat")) { 
			setStatus(fromAor, "AVAILABLE");

		} else {  
		
    		setStatus(fromAor, "AVAILABLE");
			setStatus(toAor, "AVAILABLE");
		}
	}

		protected void doMessage(SipServletRequest request) throws ServletException, IOException {
    	String aor = getSIPuri(request.getHeader("From"));
    	String toAor = getSIPuri(request.getHeader("To"));
		String fromdomain = aor.substring(aor.indexOf("@")+1, aor.length());
		System.out.println(aor);
		System.out.println(toAor);
		System.out.println(request.getContent()); //conteudo da mensagem

	//	String messageData = request.getContent().toString();
	//	System.out.println(aor);
	//	System.out.println("oo");

		if(toAor.equals("sip:gofind@acme.pt"))
           {
				  //  Proxy proxy = request.getProxy();
                //	proxy.setRecordRoute(true);ffff
                //	proxy.setSupervised(false);
                //	URI toContact = factory.createURI(request.getContent().toString());
                //	proxy.proxyTo(toContact);
				SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);

// Create SIP URIs for caller and callee
SipURI fromUri = sipFactory.createSipURI("caller", "acme.pt");
SipURI toUri = sipFactory.createSipURI("callee", "acme.pt");

// Create Addresses from URIs
Address fromAddress = sipFactory.createAddress(aor);
Address toAddress = sipFactory.createAddress(request.getContent().toString());




				SipServletRequest inviteRequest = factory.createRequest(
					request.getApplicationSession(),
					"INVITE",
					RegistrarDB.get(aor),
					RegistrarDB.get(new String(request.getRawContent()))
				
				);
			//	//inviteRequest.setRequestURI(factory.createSipURI(null,request.getContent().toString().trim()));
			//	inviteRequest.send();
                SipServletResponse response = request.createResponse(200);
        	;// 403 (forbidden response)
			//	inviteRequest.setRequestURI(factory.createURI("sip:bob@acme.pt"));
				inviteRequest.send();
				response.send();
			//	SipServletRequest inviteRequest2 = factory.createRequest(
			//		request.getApplicationSession(),
			//		"INVITE",
					
			//		factory.createURI("sip:bob@acme.pt"),
				//	factory.createURI(aor)
				
				//);
				//inviteRequest.setRequestURI(factory.createSipURI(null,request.getContent().toString().trim()));
				//inviteRequest2.send();
				inviteRequest.setRequestURI(factory.createURI("sip:bob@acme.pt"));
        	  //  response.send();// 403 (forbidden response)
            	 // Envia a mensagem
			}
		else if(!fromdomain.equals("a.pt"))
		{
							SipServletRequest inviteRequest = factory.createRequest(
					request.getApplicationSession(),
					"MESSAGE",
					toAor,
					aor
				
				);

				SipServletResponse response = request.createResponse(200);
				inviteRequest.setContent((Object) "Apenas utilizadores restritos", "text&plain");
				response.send();
				inviteRequest.send();

		}

	//	String domain = aor.substring(aor.indexOf("@") + 1, aor.length()); // Obtemos o "domain" do "aor"
      //  String contact = getSIPuriPort(request.getHeader("Contact")); // Obtemos o "contact" do request

		//	if ("a.pt".equals(domain)) { // O dominio corresponde ao pretendido
		//		RegistrarDB.put(aor, contact); // Adcionamos à BD
		//		setStatus(aor, "AVAILABLE"); // Colocamos o está do "aor" com 'AVAILABLE'
		//		request.createResponse(200).send(); // 200 (ok response)
                 // Envia a mensagem
				
		//	} else { // O dominio não corresponde ao pretendido 
		//		request.createResponse(403).send(); // 403 (forbidden response)
          //  	 // Envia a mensagem
			//}

			//request.createResponse(200).send();
											SipServletResponse response = request.createResponse(200);
        	response.send();// 403 (forbidden response)
	}

	
	/**
        * Auxiliary function for extracting SPI URIs
        * @param  uri A URI with optional extra attributes 
        * @return SIP URI 
        */
	protected String getSIPuri(String uri) {
		String f = uri.substring(uri.indexOf("<")+1, uri.indexOf(">"));
		int indexCollon = f.indexOf(":", f.indexOf("@"));
		if (indexCollon != -1) {
			f = f.substring(0,indexCollon);
		}
		return f;
	}

	/**
        * Auxiliary function for extracting SPI URIs
        * @param  uri A URI with optional extra attributes 
        * @return SIP URI and port 
        */
	protected String getSIPuriPort(String uri) {
		String f = uri.substring(uri.indexOf("<")+1, uri.indexOf(">"));
		return f;
	}

	/**
        * Auxiliary function for extracting expires valiable
        * @param  uri A URI with optional extra attributes 
        * @return expires value 
    	*/
	protected String getPortExpires(String uri) {
		String value = uri.substring(uri.indexOf("=")+1, uri.length()); // Apartir do uir "<sip:alice@a.pt:5555>;expires=3600" obtemos "3600" em string
		return value;
	}

	/**
        * Auxiliary function for changing the user Status
        * @param  userStatusMap HashMap that registers the user Status, initialized in the top of the class
        */
    private void setStatus(String user, String status) {
        userStatusMap.put(user, status);
    }

	/**
        * Auxiliary function for changing the user Status
        * @param  userStatusMap HashMap that registers the user Status, initialized in the top of the class
        * @return Status from a key
        */
	private String getStatus(String user) {
    	return userStatusMap.get(user);
    }


}