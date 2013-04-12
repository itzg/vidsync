package me.itzgeoff.vidsync.web.client;

import me.itzgeoff.vidsync.client.Receiver;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/client")
public abstract class VidSyncClientController {

    protected abstract Receiver createReceiver();
    
    @RequestMapping(method=RequestMethod.GET, produces=MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String index() {
        return "Client is ready";
    }
}
