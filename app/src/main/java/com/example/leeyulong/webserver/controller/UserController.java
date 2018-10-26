package com.example.leeyulong.webserver.controller;


import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.ResponseBody;
import com.yanzhenjie.andserver.annotation.RestController;

@RestController
public class UserController {

    @ResponseBody
    @GetMapping("/user/login")
    String login(@RequestParam("account") String account, @RequestParam("password") String password){
        if("123".equals(account) && "321".equals(password)){
            return "login success";
        }else{
            return "error login";
        }
    }


}
