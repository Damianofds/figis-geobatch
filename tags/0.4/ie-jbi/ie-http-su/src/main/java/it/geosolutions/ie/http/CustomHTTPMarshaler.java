package it.geosolutions.ie.http;

import java.io.OutputStream;
import java.util.Enumeration;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.servicemix.http.endpoints.DefaultHttpConsumerMarshaler;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.mortbay.jetty.HttpHeaders;

/**
 * Simple Consumer Marshaller
 * 
 * @author <a href="mailto:ingenieroariel@gmail.com">Ariel Nunez</a>
 * @version $Revision: 0.1 $
 */
public class CustomHTTPMarshaler extends DefaultHttpConsumerMarshaler {

	static final Logger LOGGER = Logger.getLogger(DefaultHttpConsumerMarshaler.class);
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
		LOGGER.info("Starting Exchange");
		LOGGER.info(request.getAttributeNames());
		
		// create a message exchange with the set default MEP
		MessageExchange me = context.getDeliveryChannel().createExchangeFactory().createExchange(getDefaultMep());
		
		// DEBUG: DUMP PARAMS
		Enumeration attributes = request.getAttributeNames();
		while (attributes.hasMoreElements()) {
			String att = (String) attributes.nextElement();
			LOGGER.info("REQUEST ATTRIBUTE: " + att + ":" + request.getAttribute(att));
		}

		// Fill message properties from request parameters.
		// TODO: Find out why this is not done automatically by the HTTP
		// component
//		Enumeration parameters = request.getParameterNames();
		// create the "in" message
		NormalizedMessage in = me.createMessage();
		
		in.setProperty("method", request.getMethod());

		encapsulateRequestParameters(request, in);
//		while (parameters.hasMoreElements()) {
//			String att = (String) parameters.nextElement();
//			LOGGER.info("REQUEST PARAMETER: " + att + ": " + request.getParameter(att));
//			in.setProperty(att, request.getParameter(att));
//		}
		
		me.setMessage(in, "in");
		
		// finally return the ready exchange to be sent
		return me;
	}

	/**
	 * @param request
	 * @param in
	 * @param user
	 * @throws MessagingException
	 */
	private void encapsulateRequestParameters(HttpServletRequest request,
			NormalizedMessage in) throws MessagingException {
		// set a dummy content, otherwise the NMR will throw errors - null
		// content not allowed
		in.setContent(new StringSource("<payload/>"));

		Enumeration params = request.getParameterNames();
		while(params != null && params.hasMoreElements()) {
			String paramName = (String) params.nextElement();
			String paramValue = request.getParameter(paramName);
			LOGGER.info("REQUEST PARAMETER: " + paramName + ": " + paramValue);
			in.setProperty(paramName, paramValue);
		}
		
	}
	/**
	 * 
	 */
	public void sendOut(MessageExchange exchange, NormalizedMessage outMsg,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		addResponseHeaders(exchange, request, response);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		String jsoncallback = "";
		try {
			String tmp = request.getParameter("jsoncallback");
			if (tmp != null) {
				jsoncallback = tmp;
			} else {
				String tmp2 = request.getParameter("callback");
				if (tmp2 != null) {
					jsoncallback = tmp2;
				}
			}
		} catch (Exception e) {
			// Harmless exception, nothing happens if there is no callback
		}

		OutputStream encodingStream = getResponseEncodingStream(request
				.getHeader(HttpHeaders.ACCEPT_ENCODING), response
				.getOutputStream());
		String text = jsoncallback + "(" + ((StringSource) outMsg.getContent()).getText() + ")";
		encodingStream.write(text.getBytes());
		encodingStream.close();

	}

	/**
     * 
     */
	public void sendFault(MessageExchange exchange, Fault fault,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		addResponseHeaders(exchange, request, response);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		OutputStream encodingStream = getResponseEncodingStream(request
				.getHeader(HttpHeaders.ACCEPT_ENCODING), response
				.getOutputStream());
		StringSource ss = (StringSource) fault.getContent();
		encodingStream.write(ss.getText().getBytes());
		encodingStream.close();
	}

}