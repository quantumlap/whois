package net.ripe.db.whois.logsearch;

import com.google.common.io.Files;
import net.ripe.db.whois.common.IntegrationTest;
import net.ripe.db.whois.logsearch.bootstrap.LogSearchJettyBootstrap;
import net.ripe.db.whois.logsearch.bootstrap.LogSearchJettyConfig;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Category(IntegrationTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {"classpath:applicationContext-logsearch-base.xml", "classpath:applicationContext-logsearch-test.xml"})
public class LogSearchNewFormatTestIntegration extends AbstractJUnit4SpringContextTests {

    @Autowired
    private LogSearchJettyBootstrap logSearchJettyBootstrap;
    @Autowired
    private LogSearchJettyConfig logSearchJettyConfig;
    @Autowired
    private NewLogFormatProcessor newLogFormatProcessor;
    @Autowired
    private LogFileIndex logFileIndex;

    @Autowired
    private DataSource dataSource;

    private Client client;

    private static File indexDirectory = Files.createTempDir();
    private static File logDirectory = Files.createTempDir();
    private static final String API_KEY = "DB-WHOIS-logsearchtestapikey";

    @BeforeClass
    public static void setupClass() {
        System.setProperty("dir.logsearch.index", indexDirectory.getAbsolutePath());
        System.setProperty("dir.update.audit.log", logDirectory.getAbsolutePath());

        LogsearchTestHelper.setupDatabase(new JdbcTemplate(LogsearchTestHelper.createDataSource("")), "acl.database", "ACL", "acl_schema.sql");
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty("dir.logsearch.index");
        System.clearProperty("dir.update.audit.log");
    }

    @Before
    public void setup() {
        LogFileHelper.createLogDirectory(logDirectory);
        logSearchJettyBootstrap.start();
        client = ClientBuilder.newBuilder().build();
        LogsearchTestHelper.insertApiKey(API_KEY, dataSource);
    }

    @After
    public void cleanup() {
        LogFileHelper.deleteLogs(logDirectory);
        logFileIndex.removeAll();
        logSearchJettyBootstrap.stop(true);
    }

    @Test
    public void single_term() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "the quick brown fox"));

        final String response = getUpdates("quick");

        assertThat(response, containsString("Found 1 update log(s)"));
        assertThat(response, containsString("the quick brown fox"));
    }

    @Test
    public void single_term_multiple_matches() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "inetnum: 10.0.0.0/24"));
        addToIndex(LogFileHelper.createLogFile(logDirectory, "inetnum: 10.0.0.0/24"));
        addToIndex(LogFileHelper.createLogFile(logDirectory, "inetnum: 10.0.0.0/24"));

        assertThat(getUpdates("inetnum"), containsString("Found 3 update log(s)"));
    }


    @Test
    public void no_results() throws Exception {
        assertThat(getUpdates("quick"), containsString("Found 0 update log(s)"));
    }

    @Test
    public void tarfile_single_term() throws Exception {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20100101", "100102", "the quick brown fox"));

        final String response = getUpdates("quick");

        assertThat(response, containsString("Found 1 update log(s)"));
        assertThat(response, containsString("the quick brown fox"));
    }

    @Test
    public void single_term_and_date() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "the quick brown fox"));

        assertThat(getUpdates("quick", LogFileHelper.getDate()), containsString("Found 1 update log(s)"));
    }

    @Test
    public void multiple_terms() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "the quick brown fox"));

        assertThat(getUpdates("quick fox"), containsString("Found 1 update log(s)"));
        assertThat(getUpdates("quick fox"), containsString("the quick brown fox"));
    }

    @Test
    public void single_term_inetnum_with_prefix_length() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "inetnum: 10.0.0.0/24"));

        assertThat(getUpdates("10.0.0.0/24"), containsString("inetnum: 10.0.0.0/24"));
    }

    @Test
    public void multiple_inetnum_terms() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "inetnum: 192.0.0.0 - 193.0.0.0"));

        assertThat(getUpdates("192.0.0.0 - 193.0.0.0"), containsString("192.0.0.0 - 193.0.0.0"));
    }

    @Test
    public void single_inet6num_term() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "inet6num: 2001:a08:cafe::/48"));

        assertThat(getUpdates("2001:cafe"), not(containsString("2001:a08:cafe::/48")));
    }

    @Test
    public void curly_brace_in_search_term() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "mnt-routes: ROUTES-MNT {2001::/48}"));

        assertThat(getUpdates("{2001::/48}"), containsString("ROUTES-MNT {2001::/48}"));
    }

    @Test
    public void dash_in_search_term() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "mntner: OWNER-MNT"));

        assertThat(getUpdates("OWNER-MNT"), containsString("OWNER-MNT"));
    }

    @Test
    public void search_multiple_terms_in_failed_update() throws Exception {
        addToIndex(LogFileHelper.createLogFile(logDirectory,
                        "SUMMARY OF UPDATE:\n" +
                        "\n" +
                        "Number of objects found:                   1\n" +
                        "Number of objects processed successfully:  0\n" +
                        " Create:         0\n" +
                        " Modify:         0\n" +
                        " Delete:         0\n" +
                        " No Operation:   0\n" +
                        "Number of objects processed with errors:   1\n" +
                        " Create:         1\n" +
                        " Modify:         0\n" +
                        " Delete:         0\n" +
                        "\n" +
                        "DETAILED EXPLANATION:\n" +
                        "\n" +
                        "\n" +
                        "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                        "The following object(s) were found to have ERRORS:\n" +
                        "\n" +
                        "---\n" +
                        "Create FAILED: [person] FP1-TEST   First Person\n" +
                        "\n" +
                        "person:         First Person\n" +
                        "address:        St James Street\n" +
                        "address:        Burnley\n" +
                        "address:        UK\n" +
                        "phone:          +44 282 420469\n" +
                        "nic-hdl:        FP1-TEST\n" +
                        "mnt-by:         OWNER-MNT\n" +
                        "changed:        user@ripe.net\n" +
                        "source:         TEST\n" +
                        "\n" +
                        "***Error:   Authorisation for [person] FP1-TEST failed\n" +
                        "           using \"mnt-by:\"\n" +
                        "           not authenticated by: OWNER-MNT\n" +
                        "\n\n\n" +
                        "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"));

        final String response = getUpdates("FAILED: mnt-by: OWNER-MNT");

        assertThat(response, containsString("Create FAILED: [person] FP1-TEST   First Person"));
    }

    @Test
    public void search_date_range() throws IOException, InterruptedException {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130505", "105508", "mntner: TEST-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130507", "094501", "mntner: UPD-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130508", "170601", "mntner: OTHER-MNT"));

        String response = getUpdates("mntner", "20130506", "20130509");

        assertThat(response, containsString("UPD-MNT"));
        assertThat(response, containsString("OTHER-MNT"));
        assertThat(response, not(containsString("TEST-MNT")));
    }

    @Test
    public void search_inverse_date_range() throws IOException {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130505", "105508", "mntner: TEST-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130507", "094501", "mntner: UPD-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130508", "170601", "mntner: OTHER-MNT"));

        String response = getUpdates("mntner", "20130509", "20130506");

        assertThat(response, containsString("Found 2 update log(s)"));
    }

    @Test
    public void search_date_no_results() throws IOException {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130507", "150102", "mntner: UPD-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130508", "100102", "mntner: OTHER-MNT"));

        String response = getUpdates("mntner", "20130506");

        assertThat(response, containsString("Found 0 update log(s)"));
    }

    @Test
    public void search_dates_not_specified() throws IOException {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20100305", "150640", "person: Albert K"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20110307", "173050", "person: Urban J"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20110308", "231145", "person: Wilfred D"));

        final String response = getUpdates("person");

        assertThat(response, containsString("Found 3 update log(s)"));
        assertThat(response, containsString("person: Wilfred D"));
        assertThat(response, containsString("person: Urban J"));
        assertThat(response, containsString("person: Albert K"));
    }

    @Test
    public void search_inetnum_and_date() throws IOException {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "REQUEST FROM:193.0.1.204\nPARAMS:"));

        final String response = getUpdates("193.0.1.204", LogFileHelper.getDate());

        assertThat(response, containsString("REQUEST FROM:193.0.1.204"));
    }

    @Test
    public void search_inet6num_and_date() throws IOException {
        addToIndex(LogFileHelper.createLogFile(logDirectory, "REQUEST FROM:2000:3000:4000::/48\nPARAMS:"));

        final String response = getUpdates("2000:3000:4000::/48", LogFileHelper.getDate());

        assertThat(response, containsString("REQUEST FROM:2000:3000:4000::/48"));
    }

    @Test
    public void search_incorrect_api_key() throws IOException {
        try {
            client.target(String.format(
                    "http://localhost:%s/api/logs?search=%s&apiKey=WRONG",
                    logSearchJettyConfig.getPort(), URLEncoder.encode("mntner", "ISO-8859-1")))
                .request()
                .get(String.class);
            fail();
        } catch (ForbiddenException ignored) {
            // expected
        }
    }

    @Test
    public void index_logfile_twice_not_duplicated() throws Exception {
        final File logfile = LogFileHelper.createLogFile(logDirectory, "Once upon a midnight dreary");
        addToIndex(logfile);
        addToIndex(logfile);

        final String result = getUpdates("midnight");

        assertThat(result, containsString("Found 1 update log(s)"));
    }

    @Test
    public void nonexistant_logfile_causes_error_in_search_results() throws Exception {
        final File logfile =LogFileHelper.createLogFile(logDirectory, "the quick brown fox");
        addToIndex(logfile);
        LogFileHelper.deleteLogs(logDirectory);

        assertThat(getUpdates("quick"), containsString("is neither file nor directory"));
    }

    @Test
    public void index_directory_containing_tar_files() throws Exception {
        LogFileHelper.createTarredLogFile(logDirectory, "20100101", "100102", "the quick brown fox");
        newLogFormatProcessor.addDirectoryToIndex(logDirectory.getAbsolutePath());

        final String result = getUpdates("quick");

        assertThat(result, containsString("Found 1 update log(s)"));
        assertThat(result, containsString("the quick brown fox"));
    }

    @Test
    public void index_directory_containing_tar_files_no_duplicates() throws Exception {
        LogFileHelper.createTarredLogFile(logDirectory, "20100101", "100102", "the quick brown fox");
        newLogFormatProcessor.addDirectoryToIndex(logDirectory.getAbsolutePath());
        newLogFormatProcessor.addDirectoryToIndex(logDirectory.getAbsolutePath());

        final String result = getUpdates("quick");

        assertThat(result, containsString("Found 1 update log(s)"));
        assertThat(result, containsString("the quick brown fox"));
    }

    @Test
    public void daily_update() throws Exception {
        final String yesterday = LogFileIndex.DATE_FORMATTER.print(LocalDate.now().minusDays(1));

        addToIndex(LogFileHelper.createLogFile(logDirectory, yesterday, "100102", "this is a test"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, yesterday, "100101", "this is another test"));

        newLogFormatProcessor.dailyUpdate();

        String result = getUpdates("test");

        assertThat(result, containsString("Found 1 update log(s)"));
        assertThat(result, containsString("this is another test"));
        assertThat(result, not(containsString("this is a test")));
    }

    @Test
    public void daily_update_not_indexing_msg_out_file() throws Exception {
        final String yesterday = LogFileIndex.DATE_FORMATTER.print(LocalDate.now().minusDays(1));
        final String twodaysago = LogFileIndex.DATE_FORMATTER.print(LocalDate.now().minusDays(2));

        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, yesterday, "083412", "001.msg-in.txt.gz", "inet6num: 2001:2002:2003::/48\n"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, twodaysago, "083532", "004.msg-out.txt.gz", "inet6num: 2001:2002:2003::/48\n"));

        newLogFormatProcessor.dailyUpdate();

        assertThat(getUpdates("inet6num:"), containsString("Found 1 update log"));
    }

    @Test
    public void rewrite_logfile_index_updated() throws Exception {
        final String today = LogFileIndex.DATE_FORMATTER.print(LocalDate.now());

        addToIndex(LogFileHelper.createLogFile(logDirectory, today, "100102", "random", "mntner: UPD-MNT"));
        addToIndex(LogFileHelper.createLogFile(logDirectory, today, "100102", "random", "mntner: OTHER-MNT"));

        final String response = getUpdates("mntner");

        assertThat(response, containsString("OTHER-MNT"));
        assertThat(response, not(containsString("UPD-MNT")));
    }

    @Test
    public void rewrite_tarfile_index_updated() throws Exception {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100102", "random", "mntner: UPD-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100102", "random", "mntner: OTHER-MNT"));

        final String response = getUpdates("mntner");

        assertThat(response, containsString("OTHER-MNT"));
        assertThat(response, not(containsString("UPD-MNT")));
    }

    @Test
    public void override_without_reason_is_filtered() throws Exception {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100102", "random",
                "mntner:    UPD-MNT\n" +
                "source:    RIPE\n" +
                "override:  username,password\n"));

        assertThat(getUpdates("UPD-MNT"), containsString("override: username, FILTERED\n"));
    }

    @Test
    public void override_with_reason_is_filtered() throws Exception {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100102", "random",
                "mntner:    UPD-MNT\n" +
                "source:    RIPE\n" +
                "override:  username,password,reason\n"));

        assertThat(getUpdates("UPD-MNT"), containsString("override: username, FILTERED, reason\n"));
    }

    @Test
    public void search_by_update_id_no_match() throws Exception {
        assertThat(logFileIndex.searchByUpdateId(".*20130102.*"), hasSize(0));
    }

    @Test
    public void search_by_update_id_with_match() throws Exception {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100102", "random",
                "mntner:    UPD-MNT\n" +
                "source:    RIPE\n" +
                "override:  username,password,reason\n"));

        assertThat(logFileIndex.searchByUpdateId(".*20130102.*"), hasSize(1));
    }

    @Test
    public void search_by_update_id_with_multiple_matches() throws Exception {
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100101", "random", "mntner: UPD-MNT"));
        addToIndex(LogFileHelper.createTarredLogFile(logDirectory, "20130102", "100102", "random", "mntner: UPD-MNT"));

        assertThat(logFileIndex.searchByUpdateId(".*20130102.*"), hasSize(2));
    }

    // API calls

    private String getUpdates(final String searchTerm) throws IOException {
        return client
                .target(String.format("http://localhost:%s/api/logs?search=%s&fromdate=&todate=&apiKey=%s",
                        logSearchJettyConfig.getPort(),
                        URLEncoder.encode(searchTerm, "ISO-8859-1"), API_KEY))
                .request()
                .get(String.class);
    }

    private String getUpdates(final String searchTerm, final String date) throws IOException {
        return client
                .target(String.format("http://localhost:%s/api/logs?search=%s&fromdate=%s&apiKey=%s",
                        logSearchJettyConfig.getPort(),
                        URLEncoder.encode(searchTerm, "ISO-8859-1"), date, API_KEY))
                .request()
                .get(String.class);
    }

    private String getUpdates(final String searchTerm, final String fromDate, final String toDate) throws IOException {
        return client
                .target(String.format("http://localhost:%s/api/logs?search=%s&fromdate=%s&todate=%s&apiKey=%s",
                        logSearchJettyConfig.getPort(),
                        URLEncoder.encode(searchTerm, "ISO-8859-1"), fromDate, toDate, API_KEY))
                .request()
                .get(String.class);
    }

    // helper methods

    private void addToIndex(final File file) throws IOException {
        if (file.isDirectory()) {
            newLogFormatProcessor.incrementalUpdate();
        } else {
            if (file.getAbsolutePath().endsWith(".tar")) {
                newLogFormatProcessor.addFileToIndex(file.getAbsolutePath());
            } else {
                newLogFormatProcessor.incrementalUpdate();
            }
        }
    }
}