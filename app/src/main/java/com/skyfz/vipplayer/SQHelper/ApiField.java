package com.skyfz.vipplayer.SQHelper;

public class ApiField {
    //private variables
    private long _id;
    private String _name;
    private String _url;

    public ApiField(){

    }

    // constructor
    public ApiField(long id, String name, String url){
        this._id = id;
        this._name = name;
        this._url = url;
    }

    // constructor
    public ApiField(String name, String url){
        this._name = name;
        this._url = url;
    }

    // getting ID
    public long getID(){
        return this._id;
    }

    // setting id
    public void setID(long id){
        this._id = id;
    }

    // getting name
    public String getName(){
        return this._name;
    }

    // setting name
    public void setName(String name){
        this._name = name;
    }

    // getting phone number
    public String getURL(){
        return this._url;
    }

    // setting phone number
    public void setURL(String url){
        this._url = url;
    }
}
