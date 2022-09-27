package org.coastline.one.spring.controller;

import org.coastline.one.spring.annotation.RequestMonitor;
import org.coastline.one.spring.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/item")
@RestController
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping(value = "/set")
    public ResponseEntity<String> setKey(@RequestParam("key") String key, @RequestParam("value") String value) {
        return ResponseEntity.ok(value);
    }

    @RequestMonitor
    @GetMapping(value = "/get")
    public ResponseEntity<String> getList(@RequestParam("key") String key) {
        String item = itemService.getItem(key);
        return ResponseEntity.ok(item);
    }

}