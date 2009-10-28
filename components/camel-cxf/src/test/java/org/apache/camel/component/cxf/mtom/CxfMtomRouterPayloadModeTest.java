/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.cxf.mtom;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import junit.framework.Assert;

import org.apache.camel.CamelContext;
import org.apache.camel.cxf.mtom_feature.Hello;
import org.apache.camel.cxf.mtom_feature.HelloService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for exercising MTOM enabled end-to-end router in PAYLOAD mode
 * 
 * @version $Revision$
 */
@ContextConfiguration
public class CxfMtomRouterPayloadModeTest extends AbstractJUnit4SpringContextTests {
    
    protected final QName serviceName = new QName("http://apache.org/camel/cxf/mtom_feature", "HelloService");
    
    @Autowired
    protected CamelContext context;
    private Endpoint endpoint;

    @Before
    public void setUp() throws Exception {
        endpoint = Endpoint.publish("http://localhost:9092/jaxws-mtom/hello", new HelloImpl());
        SOAPBinding binding = (SOAPBinding)endpoint.getBinding();
        binding.setMTOMEnabled(true);
        
    }
    
    @After
    public void tearDown() throws Exception {
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    @Test
    public void testInvokingServiceFromCXFClient() throws Exception {        
        
        if (Boolean.getBoolean("java.awt.headless") || System.getProperty("os.name").startsWith("Mac OS")) {
            System.out.println("Running headless. Skipping test as Images may not work.");
            return;
        }        
        
        Holder<byte[]> photo = new Holder<byte[]>(MtomTestHelper.REQ_PHOTO_DATA);
        Holder<Image> image = new Holder<Image>(getImage("/java.jpg"));

        Hello port = getPort();

        SOAPBinding binding = (SOAPBinding) ((BindingProvider)port).getBinding();
        binding.setMTOMEnabled(true);

        port.detail(photo, image);
        
        MtomTestHelper.assertEquals(MtomTestHelper.RESP_PHOTO_DATA,  photo.value);      
        Assert.assertNotNull(image.value);
        if (image.value instanceof BufferedImage) {
            Assert.assertEquals(560, ((BufferedImage)image.value).getWidth());
            Assert.assertEquals(300, ((BufferedImage)image.value).getHeight());            
        }  
        
    }
    
    private Hello getPort() {
        URL wsdl = getClass().getResource("/mtom.wsdl");
        assertNotNull("WSDL is null", wsdl);

        HelloService service = new HelloService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service.getHelloPort();
    }
    
    private Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }
    

}
