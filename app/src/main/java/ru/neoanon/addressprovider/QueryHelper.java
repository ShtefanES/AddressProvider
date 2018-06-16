package ru.neoanon.addressprovider;

public class QueryHelper {
    private String regionAndTown;
    private String region;

    public QueryHelper(String regionAndTown, String region) {
        this.regionAndTown = regionAndTown;
        this.region = region;
    }

    public String getQueryWithRegion(String query) {
        return regionAndTown + query;
    }

    public String cutResultQuery(String result) {
        if (result.length() <= regionAndTown.length()) {
            return null;
        } else if (result.substring(0, regionAndTown.length()).equals(regionAndTown)) {
            return result.substring(regionAndTown.length() + 1);
        } else if (result.substring(0, region.length()).equals(region)) {
            return result.substring(region.length() + 1);
        }
        return result;
    }
}
