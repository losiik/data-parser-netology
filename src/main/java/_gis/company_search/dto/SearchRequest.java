package _gis.company_search.dto;

public class SearchRequest {
    private String city;
    private String placeName;

    public String getCity() {
        return city;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setCity(String city){
        this.city = city;
    }

    public void setPlaceName(String placeName){
        this.placeName = placeName;
    }
}