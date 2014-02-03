/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package diuf.unifr.ch.first.xwot.rxtx.notifications;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import diuf.unifr.ch.first.xwot.rxtx.test.utils.SerialHelpers;

/**
 *
 * @author leo
 */
public class NotificationBuilderTest extends SerialHelpers{

    /**
     * Test of jaxbToXml method, of class NotificationBuilder.
     */
    @Test
    public void testJaxbToXml() {
        NotificationBuilderImpl impl = new NotificationBuilderImpl();
        TestClient c = new TestClient();
        String hello = "hello";
        c.setUri(hello);
        String r = impl.jaxbToXml(TestClient.class, c);
        assertResponseContains(r, "<testClient");
        assertResponseContains(r, "<uri>hello</uri>");
        assertResponseContains(r, "</testClient>");
    }

    /**
     * Test of jaxbToJson method, of class NotificationBuilder.
     */
    @Test
    public void testJaxbToJson() {
        NotificationBuilderImpl impl = new NotificationBuilderImpl();
        TestClient c = new TestClient();
        String hello = "hello";
        c.setUri(hello);
        String r = impl.jaxbToJson(c);
        assertResponseContains(r, "{\"uri\":\"hello\"}");
    }

    class NotificationBuilderImpl extends NotificationBuilder {

        @Override
        public boolean hasNotification() {
            return false;
        }

        @Override
        public StringEntity jaxbToStringEntity(Object client) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
