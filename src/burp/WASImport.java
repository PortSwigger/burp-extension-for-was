package burp;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class WASImport {

	private File xmlFile;
	private long webappid;
	private String portal_username = "";
	private String portal_password = "";
	private SimpleDateFormat time_formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	private IBurpExtenderCallbacks callbacks;
	private String post_Url;
	private boolean isPurgeIssues;
	private boolean isCloseIssues;
	
	private static String parsing_burpxml_success_code = "SUCCESS";
	
	  
	public WASImport(File xmlFile, long webappid, String portal_username, String portal_password,
			IBurpExtenderCallbacks callbacks, String post_Url, boolean isPurgeIssues, boolean isCloseIssues) {
		super();
		this.xmlFile = xmlFile;
		this.webappid = webappid;
		this.portal_username = portal_username;
		this.portal_password = portal_password;
		this.callbacks = callbacks;
		this.post_Url = post_Url;
		this.isPurgeIssues = isPurgeIssues;
		this.isCloseIssues = isCloseIssues;
	}

 
	
	private void disableSSLCertificateChecking()
	  {
	    TrustManager[] trustAllCerts = { new X509TrustManager() {
	      public X509Certificate[] getAcceptedIssuers() { return null; }

	      public void checkClientTrusted(X509Certificate[] arg0, String arg1)
	        throws CertificateException
	      {}
	      


	      public void checkServerTrusted(X509Certificate[] arg0, String arg1)
	        throws CertificateException
	      {}
	    } };
	    
	    try
	    {
	      SSLContext sc = SSLContext.getInstance("TLS");
	      
	      sc.init(null, trustAllCerts, new SecureRandom());
	      
	      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	    } catch (KeyManagementException e) {
	        BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception while procesing SSL certificate checking; " + e.getMessage()+ "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    } catch (NoSuchAlgorithmException e) {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception while procesing SSL certificate checking; " + e.getMessage()+ "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    }
	  }
	
	
	 public String getResponseCode(String response)
	  {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
	    String response_message_code = "";
	    try {
	      DocumentBuilder builder = factory.newDocumentBuilder();
	      Document doc = builder.parse(new InputSource(new StringReader(response)));
	      NodeList listofErrorResponse = doc.getElementsByTagName("responseCode");
	      Node responseMessage = listofErrorResponse.item(0);
	      if (responseMessage.getNodeType() == 1) {
	        response_message_code = responseMessage.getFirstChild().getNodeValue();
	      }
	    }
	    catch (Exception e)
	    {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception ; " + e.getMessage()+ "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    	return "";
	    }
	    
	    return response_message_code;
	  }
	 
	  public boolean checkExportStatus(String response)
	  {
	    String parse_burpXML_response = getResponseCode(response);
	    if (parse_burpXML_response.equals(parsing_burpxml_success_code))
	      return true;
	    return false;
	  }
	  
	 public String sendXMLtoPortal()
	  {
	  
			String responseData = "";
		    disableSSLCertificateChecking();    
		  
		    try {
		    	
		    	IExtensionHelpers helpers = callbacks.getHelpers();
		    	URL url = new URL(post_Url);
		    	
		    	ArrayList<String> headers = new ArrayList<String>();
		    	headers.add("user: " + portal_username);
		    	headers.add("password: "+ portal_password);
		    	
		    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Purge issues before import : " + isPurgeIssues + " ; Close existing issues : " + isCloseIssues + "\n");
			  	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
			  	  
		    	String postXMLData = constructRequestParameterXMLData(isPurgeIssues, isCloseIssues);
		    	
		    	byte[] httpHeaders = helpers.buildHttpMessage(headers, postXMLData.getBytes());
		    	   	
		    	byte[] requestProps = callbacks.getHelpers().stringToBytes("POST "+url.getPath()+" HTTP/1.1\r\n"+"Content-Type: text/xml"+"\r\n" +"Content-Language: en-US"+"\r\n");
		    	
		    	
		    	byte[] hostHeaders = callbacks.getHelpers().stringToBytes("Host: "+url.getHost()+"\r\n");
		    	
		    	String buildRequest = new String(requestProps) + new String(hostHeaders) + new String(httpHeaders);
		    	
		        String protocol = url.getProtocol();
		        Boolean isSSL = (protocol.equals("https"));
		        
		        BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Making HTTP Request to : " + post_Url + "\n");
		    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
		    	
		        byte[] response = callbacks.makeHttpRequest(url.getHost(), url.getPort() == -1 ? 443 : url.getPort(), isSSL, buildRequest.getBytes());
       
		        IResponseInfo responseInfo = helpers.analyzeResponse(response);
		        
		        int offset = responseInfo.getBodyOffset();
		        byte[] responseBody = Arrays.copyOfRange(response, offset, response.length);
		        responseData = new String(responseBody);
		        return responseData;
		    }
		    catch (Exception e) {
		      BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception importing burp issues; " + e.getMessage()+ "\n" + "###### Response Data ##### \n" +responseData+"\n");
		  	  BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
		    } 
		    return responseData;
		  
	  }
	  
	  public String constructRequestParameterXMLData(boolean isPurgeIssues, boolean isCloseIssues)
	  {
	    String postData = "";
	    String fileName = xmlFile.getName();
	    try
	    {
	      String burpXMLContent = new String(Files.readAllBytes(Paths.get(xmlFile.getAbsolutePath(), new String[0])));
	      
	      DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
	      DocumentBuilder build = dFact.newDocumentBuilder();
	      Document doc = build.newDocument();
	      

	      Element root = doc.createElement("ServiceRequest");
	      doc.appendChild(root);
	      Element data = doc.createElement("data");
	      root.appendChild(data);
	      

	      Element webAppID_element = doc.createElement("webAppId");
	      webAppID_element.appendChild(doc.createTextNode(webappid+""));
	      data.appendChild(webAppID_element);
	      
	      Element purgeResults_element = doc.createElement("purgeResults");
	      purgeResults_element.appendChild(doc.createTextNode(String.valueOf(isPurgeIssues)));
	      data.appendChild(purgeResults_element);
	      
	      Element closeUnreportedIssues_element = doc.createElement("closeUnreportedIssues");
	      closeUnreportedIssues_element.appendChild(doc.createTextNode(String.valueOf(isCloseIssues)));
	      data.appendChild(closeUnreportedIssues_element);
	      
	      Element fileName_element = doc.createElement("fileName");
	      fileName_element.appendChild(doc.createTextNode(fileName));
	      data.appendChild(fileName_element);
	      
	      Element burpXml_element = doc.createElement("burpXml");
	      Element issues_node = doc.createElement("issues");
	      burpXml_element.appendChild(issues_node);
	      data.appendChild(burpXml_element);
	      
	      TransformerFactory tFact = TransformerFactory.newInstance();
	      Transformer trans = tFact.newTransformer();
	      StringWriter writer = new StringWriter();
	      StreamResult result = new StreamResult(writer);
	      DOMSource source = new DOMSource(doc);
	      trans.transform(source, result);
	      postData = writer.toString();
	      postData = postData.substring(postData.indexOf(">") + 1, postData.length());
	      postData = postData.replaceAll("<issues/>", burpXMLContent);
	    
	    }
	    catch (ParserConfigurationException ex) {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception while constructing Qualys specific data for export; " + ex.getMessage() + "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    } catch (TransformerException ex) {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception while constructing Qualys specific data for export; " + ex.getMessage() + "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    }
	    catch (IOException e) {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Exception while constructing Qualys specific data for export; " + e.getMessage() + "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    }
	    return postData;
	  }

	  
	  public int parseFailedImports(String response)
	  {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    try
	    {
	      DocumentBuilder builder = factory.newDocumentBuilder();
	      Document doc = builder.parse(new InputSource(new StringReader(response)));
	      Node errorRecords = doc.getElementsByTagName("errorRecords").item(0);
	      
	      Element webAppElement = (Element)errorRecords;
          Node count_Node = webAppElement.getElementsByTagName("count").item(0);
          String failure = count_Node.getFirstChild().getNodeValue();
	      int failCount = Integer.parseInt(failure);
	      return failCount;
	    }
	    catch (Exception e) {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Error parsing the export API response; exception = " + e.getMessage()+ "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    }
	    return 0;
	  }
	  
	  public int parseSuccessImports(String response)
	  {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    try
	    {
	      DocumentBuilder builder = factory.newDocumentBuilder();
	      Document doc = builder.parse(new InputSource(new StringReader(response)));
	      Node issuesCount = doc.getElementsByTagName("issuesCount").item(0);
	      String successful = issuesCount.getFirstChild().getNodeValue();
	      int successCount = Integer.parseInt(successful);
	      return successCount;
	    }
	    catch (Exception e) {
	    	BurpExtender.logBuilder.append(time_formatter.format(System.currentTimeMillis()) + " : Error parsing the export API response; exception = " + e.getMessage()+ "\n");
	    	BurpExtender.logTextArea.setText(BurpExtender.logBuilder.toString());
	    }
	    return 0;
	  }
	  
}
