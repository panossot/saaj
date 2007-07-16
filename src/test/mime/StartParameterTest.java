/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package mime;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.activation.DataHandler;
import javax.xml.soap.*;

import junit.framework.TestCase;

public class StartParameterTest extends TestCase {

    public StartParameterTest(String name) {
        super(name);
    }
    
    public void changeAndSaveMimeHeaders(SOAPMessage msg, String fileName)
    throws IOException {
                
        FileOutputStream fos = new FileOutputStream(fileName);        
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        
        Hashtable hashTable = new Hashtable();
        MimeHeaders mimeHeaders = msg.getMimeHeaders();
        Iterator iterator = mimeHeaders.getAllHeaders();
        
        while(iterator.hasNext()) {
            MimeHeader mimeHeader = (MimeHeader) iterator.next();
            if(mimeHeader.getName().equals("Content-Type"))
                hashTable.put(mimeHeader.getName(),
                              mimeHeader.getValue()
                              + "; start=attachmentPart");
            else
                hashTable.put(mimeHeader.getName(), mimeHeader.getValue());
        }
        
        oos.writeObject(hashTable);
        oos.flush();
        oos.close();
        
        fos.flush();
        fos.close();
    }
    
    public SOAPMessage getModifiedMessage(String mimeHdrsFile, String msgFile)
    throws Exception {
        SOAPMessage message;
        
        MimeHeaders mimeHeaders = new MimeHeaders();
        FileInputStream fis = new FileInputStream(msgFile);
        
        ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream(mimeHdrsFile));
        Hashtable hashTable = (Hashtable) ois.readObject();
        ois.close();
        
        if(hashTable.isEmpty())
            fail("MimeHeaders Hashtable is empty");
        else {
            for(int i=0; i < hashTable.size(); i++) {
                Enumeration keys = hashTable.keys();
                Enumeration values = hashTable.elements();
                while (keys.hasMoreElements() && values.hasMoreElements()) {
                    String name = (String) keys.nextElement();
                    String value = (String) values.nextElement();
                    mimeHeaders.addHeader(name, value);
                }
            }
        }
        
        MessageFactory messageFactory = MessageFactory.newInstance();
        message = messageFactory.createMessage(mimeHeaders, fis);
        
        message.saveChanges();
        
        return message;
    }

    public void testStartParameter() throws Exception {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage msg = mf.createMessage();        
        SOAPPart sp = msg.getSOAPPart();
        sp.setContentId("soapPart");
        
        SOAPEnvelope envelope = sp.getEnvelope();
       
        SOAPHeader hdr = envelope.getHeader();
        SOAPBody bdy = envelope.getBody();

        // Add to body 
        SOAPBodyElement gltp = bdy.addBodyElement(
                       envelope.createName("GetLastTradePrice", "ztrade",
				"http://wombat.ztrade.com"));
        
        gltp.addChildElement(envelope.createName("symbol", "ztrade",
                       "http://wombat.ztrade.com")).addTextNode("SUNW");
        
        // Attach an xml file containing empty Body message     
        URL url = new URL("file", null, "src/test/mime/data/message.xml");

        AttachmentPart ap = msg.createAttachmentPart(new DataHandler(url));

        ap.setContentType("text/xml");
        ap.setContentId("attachmentPart");

        msg.addAttachmentPart(ap);
 
        msg.saveChanges();

	FileOutputStream sentFile =
            new FileOutputStream("src/test/mime/data/message.txt");
	msg.writeTo(sentFile);
	sentFile.close();
        changeAndSaveMimeHeaders(msg, "src/test/mime/data/headers.txt");

        SOAPMessage newMsg = 
            getModifiedMessage("src/test/mime/data/headers.txt", 
                               "src/test/mime/data/message.txt");
        assertFalse("newMsg has an empty body",
                    newMsg.getSOAPBody().getChildElements().hasNext());
        assertTrue("Soap part has the Content-Id: attachmentPart",
                   newMsg.getSOAPPart()
                   .getContentId().equals("attachmentPart"));
        assertTrue("Attachment part has the Content-Id: soapPart",
                   ((AttachmentPart) newMsg.getAttachments().next())
                   .getContentId().equals("soapPart"));
    }

    public void testSampleMessageOne() throws Exception {
        MimeHeaders hdrs = new MimeHeaders();
        hdrs.addHeader("Server","WebSphere Application Server/5.1");
        hdrs.addHeader("Content-Type","multipart/related; " +
                       "type=\"text/xml\"; " +
                       "start=\"<139912840220.1065629194743.IBM.WEBSERVICES@ibm-7pr28r4m35k>\";         " +
                       "boundary=\"----=_Part_4_910054940.1065629194743\"");
        hdrs.addHeader("Content-Language","en-US");
        hdrs.addHeader("Connection","close");

        FileInputStream fis = 
            new FileInputStream("src/test/mime/data/msg.txt");
        MessageFactory factory =  MessageFactory.newInstance();
        SOAPMessage msg = factory.createMessage(hdrs,fis);
        String[] s = msg.getSOAPPart().getMimeHeader("Content-Description");
    }

    public void testContentDescription() throws Exception {
        MimeHeaders hdrs = new MimeHeaders();
        hdrs.addHeader("Server","WebSphere Application Server/5.1");
        hdrs.addHeader("Content-Type","multipart/related; " +
                       "type=\"text/xml\"; " +
                       "start=\"<139912840220.1065629194743.IBM.WEBSERVICES@ibm-7pr28r4m35k>\";         " +
                       "boundary=\"----=_Part_4_910054940.1065629194743\"");
        hdrs.addHeader("Content-Language","en-US");
        hdrs.addHeader("Connection","close");

        FileInputStream fis = 
            new FileInputStream("src/test/mime/data/msg.txt");
        MessageFactory factory =  MessageFactory.newInstance();
        try {
            factory.createMessage(hdrs,fis);
        } catch(Exception e) {
            fail("Exception should not be thrown " +
                 "while internalizing the message");
        }
    }

    public void testSampleMessageOneWithoutTypeParameter() throws Exception {
        MimeHeaders hdrs = new MimeHeaders();
        hdrs.addHeader("Server","WebSphere Application Server/5.1");
        hdrs.addHeader("Content-Type","multipart/related; " +
                       "start=\"<139912840220.1065629194743.IBM.WEBSERVICES@ibm-7pr28r4m35k>\";         " +
                       "boundary=\"----=_Part_4_910054940.1065629194743\"");
        hdrs.addHeader("Content-Language","en-US");
        hdrs.addHeader("Connection","close");

        FileInputStream fis = 
            new FileInputStream("src/test/mime/data/msg.txt");
        MessageFactory factory =  MessageFactory.newInstance();
        try {
            factory.createMessage(hdrs,fis);
        } catch(Exception e) {
            fail("Exception should not be thrown " +
                 "while internalizing the message");
        }
    } 

    public void testSampleMessageTwo() throws Exception {
        MimeHeaders hdrs = new MimeHeaders();
        hdrs.addHeader("Server","WebSphere Application Server/5.1");
        hdrs.addHeader("Content-Type","multipart/related; " +
                       "type=\"text/xml\"; " +
                       "start=\"<1071294019496.1066069460327.IBM.WEBSERVICES@ibm-7pr28r4m35k>\";         " +
                       "boundary=\"----=_Part_1_807283631.1066069460327\"");
        hdrs.addHeader("Content-Language","en-US");
        hdrs.addHeader("Connection","close");

        FileInputStream fis = 
            new FileInputStream("src/test/mime/data/msg2.txt");
        MessageFactory factory =  MessageFactory.newInstance();
        factory.createMessage(hdrs,fis);
    }
}
