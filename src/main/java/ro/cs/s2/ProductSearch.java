package ro.cs.s2;

import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import ro.cs.s2.util.Logger;
import ro.cs.s2.util.NetUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that issues queries to ESA's SciHub for retrieving product names.
 *
 * @author Cosmin Cara
 */
public class ProductSearch {

    private URI url;
    private List<NameValuePair> params;
    private Polygon2D polygon;
    private String filter;
    //private CredentialsProvider credsProvider;
    private UsernamePasswordCredentials credentials;
    private double cloudFilter;

    public ProductSearch(String url) throws URISyntaxException {
        this.url = new URI(url);
        this.filter = "platformName:Sentinel-2";
        this.params = new ArrayList<>();
    }

    public void setPolygon(Polygon2D polygon) {
        this.polygon = polygon;
    }

    public void setClouds(double clouds) {
        this.cloudFilter = clouds;
    }

    public ProductSearch filter(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            this.filter += " AND " + key + ":" + value;
        }
        return this;
    }

    public ProductSearch limit(int number) {
        if (number > 0) {
            params.add(new BasicNameValuePair("rows", String.valueOf(number)));
        }
        return this;
    }

    public ProductSearch start(int start) {
        if (start >= 0) {
            params.add(new BasicNameValuePair("start",String.valueOf(start)));
        }
        return this;
    }

    public ProductSearch auth(String user, String pwd) {
        this.credentials = new UsernamePasswordCredentials(user, pwd);
        return this;
    }

    public String getQuery() {
        params.add(new BasicNameValuePair("q", filter));
        return this.url.toString() + "?" + URLEncodedUtils.format(params, "UTF-8").replace("+", "%20");
    }

    public List<ProductDescriptor> execute() throws IOException {
        List<ProductDescriptor> results = new ArrayList<>();
        if (this.polygon.getNumPoints() > 0) {
            filter("footprint", "\"Intersects(" + (polygon.getNumPoints() < 200 ? polygon.toWKT() : polygon.toWKTBounds()) + ")\"");
        }
        String queryUrl = getQuery();
        Logger.info(queryUrl);
        try (CloseableHttpResponse response = NetUtils.openConnection(queryUrl, credentials)) {
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    String[] strings = EntityUtils.toString(response.getEntity()).split("\n");
                    ProductDescriptor currentProduct = null;
                    double currentClouds;
                    for (String string : strings) {
                        if (string.contains("<entry>")) {
                            currentProduct = new ProductDescriptor();
                        } else if (string.contains("</entry>")) {
                            if (currentProduct != null) {
                                double cloudsPercentage = currentProduct.getCloudsPercentage();
                                if (cloudFilter == 0 || cloudsPercentage <= cloudFilter) {
                                    results.add(currentProduct);
                                } else {
                                    Logger.info("%s skipped [clouds: %s]", currentProduct, cloudsPercentage);
                                }
                            }
                        } else if (string.contains("<title>")) {
                            if (currentProduct != null) {
                                currentProduct.setName(string.replace("<title>", "").replace("</title>", ""));
                            }
                        } else if (string.contains("cloudcoverpercentage")) {
                            currentClouds = Double.parseDouble(string.replace("<double name=\"cloudcoverpercentage\">", "").replace("</double>", ""));
                            if (currentProduct != null) {
                                currentProduct.setCloudsPercentage(currentClouds);
                            }
                        } else if (string.contains("<id>")) {
                            if (currentProduct != null) {
                                currentProduct.setId(string.replace("<id>", "").replace("</id>", ""));
                            }
                        }
                    }
                    break;
                case 401:
                    Logger.info("The supplied credentials are invalid!");
                    break;
                default:
                    Logger.info("The request was not successful. Reason: %s", response.getStatusLine().getReasonPhrase());
                    break;
            }
        }
        Logger.info("Query returned %s products", results.size());
        return results;
    }

}
