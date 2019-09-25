package com;

import com.revolut.task.Application;
import com.revolut.task.data.Account;
import com.revolut.task.protocol.Request;
import com.revolut.task.service.AccountService;
import com.revolut.task.utils.JsonUtils;
import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Test AccountService
 */
public class TestAccountService extends TestCase {

    private final HttpClient client = buildHttpClient();

    public void testCreateAccount() {
        AccountService service = new AccountService();
        Account testCreateAccount_1 = service.createAccount("testCreateAccount_1", BigDecimal.TEN);
        assertEquals("testCreateAccount_1", testCreateAccount_1.getName());

        BigDecimal sum = service.getBalance(testCreateAccount_1.getId());
        assertTrue(BigDecimal.TEN.compareTo(sum) == 0);
    }

    public void testTransfer() {
        AccountService service = new AccountService();
        Account account_1 = service.createAccount("account_1", BigDecimal.TEN);
        Account account_2 = service.createAccount("account_2", BigDecimal.TEN);

        String result = service.transfer("corrId_1", account_1.getId(), account_2.getId(), BigDecimal.TEN);
        assertEquals("Success", result);

        BigDecimal account_1_sum = service.getBalance(account_1.getId());
        assertTrue(BigDecimal.ZERO.compareTo(account_1_sum) == 0);

        BigDecimal account_2_sum = service.getBalance(account_2.getId());
        assertTrue(BigDecimal.valueOf(20).compareTo(account_2_sum) == 0);
    }

    /**
     * Test money transfer idempotency
     */
    public void testTransferDuplicate() {
        AccountService service = new AccountService();
        Account account_1 = service.createAccount("account_1", BigDecimal.TEN);
        Account account_2 = service.createAccount("account_2", BigDecimal.TEN);

        String result = service.transfer("corrId_1", account_1.getId(), account_2.getId(), BigDecimal.ONE);
        assertEquals("Success", result);

        String resultDuplicate = service.transfer("corrId_1", account_1.getId(), account_2.getId(), BigDecimal.ONE);
        assertEquals("Duplicate operation was rejected", resultDuplicate);
    }

    /**
     * Test money transfers in concurrent environment
     *
     * @throws InterruptedException
     */
    public void testMultipleTransfers() throws InterruptedException {
        final AccountService service = new AccountService();
        Account account_1 = service.createAccount("account_1", BigDecimal.valueOf(100));
        Account account_2 = service.createAccount("account_2", BigDecimal.ZERO);
        List<Thread> threadList = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            final int j = i;
            threadList.add(new Thread(() -> {
                String correlationId = String.format("corrId_%d", j);
                service.transfer(correlationId, account_1.getId(), account_2.getId(), BigDecimal.ONE);
            }));
        }
        for (Thread thread : threadList) {
            thread.start();
        }
        for (Thread thread : threadList) {
            thread.join();
        }
        BigDecimal account_1_sum = service.getBalance(account_1.getId());
        BigDecimal account_2_sum = service.getBalance(account_2.getId());

        assertTrue(account_1_sum.toString(), BigDecimal.ZERO.compareTo(account_1_sum) == 0);
        assertTrue(account_2_sum.toString(), BigDecimal.valueOf(100).compareTo(account_2_sum) == 0);
    }

    /**
     * Test money transfer through http request
     *
     * @throws IOException
     */
    public void testHttpServerTransfer() throws IOException {
        Application a = new Application(8001);
        try {
            a.start();

            Account account_1 = createAccount("account_1", "100.000001");

            assertEquals("account_1", account_1.getName());
            assertTrue("Expected: 100.000001", BigDecimal.valueOf(100.000001).compareTo(account_1.getBalance()) == 0);

            Account account_2 = createAccount("account_2", "0");

            String transferBody = String.format("{" +
                    " \"correlationId\":\"corrId_1\"," +
                    " \"fromId\":\"%s\"," +
                    " \"toId\":\"%s\"," +
                    " \"sum\":\"50\"" +
                    "}"
                    , account_1.getId(), account_2.getId());

            String transferStatus = post("http://localhost:8001/api/transfer", transferBody, String.class);

            assertEquals("Success", transferStatus);

            BigDecimal balance_1 = getBalance(account_1);
            assertTrue("Expected: 50.000001", BigDecimal.valueOf(50.000001).compareTo(balance_1) == 0);

            BigDecimal balance_2 = getBalance(account_2);
            assertTrue("Expected: 50", BigDecimal.valueOf(50).compareTo(balance_2) == 0);
        } finally {
            a.stop();
        }
    }

    private static HttpClient buildHttpClient() {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 1000);
        httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
        httpParams.setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET, HTTP.UTF_8);

        return new DefaultHttpClient(httpParams);
    }

    private <T> T post(String url, String body, Class<T> resultType) throws IOException {
        HttpPost post = new HttpPost(url);
        Request request = new Request();
        request.setBody(body);

        String json = JsonUtils.toJson(request);
        post.setEntity(new StringEntity(json, "application/json", HTTP.UTF_8));

        HttpResponse response = client.execute(post);
        InputStreamReader is = new InputStreamReader(response.getEntity().getContent(), HTTP.UTF_8);

        T result = JsonUtils.fromJson(is, resultType);
        EntityUtils.consume(response.getEntity());
        return result;
    }

    private Account createAccount(String name, String sum) throws IOException {
        String body = String.format("{\"name\":\"%s\", \"sum\":\"%s\"}", name, sum);
        return post("http://localhost:8001/api/createAccount", body, Account.class);
    }

    private BigDecimal getBalance(Account account) throws IOException {
        String body = String.format("{\"accountId\":\"%s\"}", account.getId());
        return post("http://localhost:8001/api/getBalance", body, BigDecimal.class);
    }
}
