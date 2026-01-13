package com.song.my_ipim;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/")
    public String home(){
        return "My-ipim is runing";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

}
