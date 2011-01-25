/*
 * This file is part of the DITA Open Toolkit project hosted on
 * Sourceforge.net. See the accompanying license.txt file for 
 * applicable licenses.
 */

/*
 * (c) Copyright IBM Corp. 2005, 2006 All Rights Reserved.
 */
package org.dita.dost.platform;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.dita.dost.log.DITAOTJavaLogger;
import org.dita.dost.util.Constants;
import org.dita.dost.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * InsertAction implements IAction and insert the resource 
 * provided by plug-ins into the xsl files, ant scripts and xml catalog.
 * @author Zhang, Yuan Peng
 */
public class InsertAction extends DefaultHandler2 implements IAction {

	protected final XMLReader reader;
	protected final DITAOTJavaLogger logger;
	protected final Set<String> fileNameSet;
	protected final StringBuffer retBuf;
	protected final Hashtable<String,String> paramTable;
	protected int elemLevel = 0;
	protected boolean inCdataSection = false;
	/** Current processing file. */
	protected String currentFile;
	
	/**
	 * Default Constructor.
	 */
	public InsertAction() {
		fileNameSet = new LinkedHashSet<String>(Constants.INT_16);
		logger = new DITAOTJavaLogger();
		retBuf = new StringBuffer(Constants.INT_4096);
		paramTable = new Hashtable<String,String>();
		try {
            if (System.getProperty(Constants.SAX_DRIVER_PROPERTY) == null){
                //The default sax driver is set to xerces's sax driver
            	StringUtils.initSaxDriver();
            }
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(this);
            reader.setFeature(Constants.FEATURE_NAMESPACE_PREFIX, true);
            //added by Alan for bug: #2893316 on Date: 2009-11-09 begin
            reader.setProperty(Constants.LEXICAL_HANDLER_PROPERTY, this);
            //added by Alan for bug: #2893316 on Date: 2009-11-09 end
            
            //Edited by william on 2009-11-8 for ampbug:2893664 start
			reader.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
			reader.setFeature("http://apache.org/xml/features/scanner/notify-builtin-refs", true);
			//Edited by william on 2009-11-8 for ampbug:2893664 end
			
        } catch (final Exception e) {
        	throw new RuntimeException("Failed to initialize parser: " + e.getMessage(), e);
        }
	}

	public void setInput(final String input) {
		final StringTokenizer inputTokenizer = new StringTokenizer(input, Integrator.FEAT_VALUE_SEPARATOR);
		while(inputTokenizer.hasMoreElements()){
			fileNameSet.add(inputTokenizer.nextToken());
		}
	}

	public void addParam(final String name, final String value) {
		paramTable.put(name, value);
	}

	public String getResult() {
		try{
			for (final String fileName: fileNameSet) {
				currentFile = fileName;
				reader.parse(currentFile);
			}
		} catch (final Exception e) {
	       	logger.logException(e);
		}
		return retBuf.toString();
	}

	public void setFeatures(final Hashtable<String,String> h) {
		// NOOP
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		if(elemLevel != 0){
			final int attLen = attributes.getLength();
			retBuf.append(Constants.LINE_SEPARATOR);
			retBuf.append("<").append(qName);
			for (int i = 0; i < attLen; i++){
				retBuf.append(" ").append(attributes.getQName(i)).append("=\"");
				retBuf.append(StringUtils.escapeXML(attributes.getValue(i))).append("\"");
			}
			//Added by Jason on 2010-05-09 for bug:2974667 start
			if(("public".equals(localName) ||
					"system".equals(localName) ||
					"uri".equals(localName))){
				retBuf.append("/>");
			}
			//Added by Jason on 2010-05-09 for bug:2974667 end
			else{
				retBuf.append(">");
			}
		}
		elemLevel ++;
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		if (inCdataSection) {
			retBuf.append(ch, start, length);
		} else {
    		final char[] esc = StringUtils.escapeXML(ch, start, length).toCharArray();
    		retBuf.append(esc, 0, esc.length);
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		elemLevel --;
		//edited by william on 2010-03-23 for bug:2974667 start
		if(elemLevel != 0 && 
				(!"public".equals(localName) &&
				 !"system".equals(localName) &&
				 !"uri".equals(localName))
		){
		//edited by william on 2010-03-23 for bug:2974667 end
			//remove line break bug:3062912
			//retBuf.append(Constants.LINE_SEPARATOR);
			retBuf.append("</").append(qName).append(">");
		}
	}

	@Override
	public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
		retBuf.append(ch, start, length);
	}

	@Override
	public void startDocument() throws SAXException {
		elemLevel = 0;
	}

	@Override
	public void comment(final char[] ch, final int start, final int length) throws SAXException {
		retBuf.append("<!--").append(ch, start, length).append("-->");
	}
	
	//added by Alan for bug: #2893316 on Date: 2009-11-09 begin
	@Override
	public void startCDATA() throws SAXException {
		inCdataSection = true;
		retBuf.append(Constants.CDATA_HEAD);

	}

	@Override
	public void endCDATA() throws SAXException {
		retBuf.append(Constants.CDATA_END);
		inCdataSection = false;
	}
	//added by Alan for bug: #2893316 on Date: 2009-11-09 end
}
