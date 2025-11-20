package _gis.company_search.dto;

import _gis.company_search.entity.UserEntity;

public class UserResponse {
    private String name;
    private String email;
    private Long id;

    public UserResponse(UserEntity user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setId(Long id){
        this.id = id;
    }
}
