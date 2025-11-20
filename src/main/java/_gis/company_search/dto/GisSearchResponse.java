package _gis.company_search.dto;

import java.util.ArrayList;
import java.util.List;

public class GisSearchResponse {

    private List<GisItem> items;

    public GisSearchResponse() {
        this.items = new ArrayList<>();
    }

    public GisSearchResponse(List<GisItem> items) {
        this.items = items;
    }

    public List<GisItem> getItems() {
        return items;
    }

    public void setItems(List<GisItem> items) {
        this.items = items;
    }
}