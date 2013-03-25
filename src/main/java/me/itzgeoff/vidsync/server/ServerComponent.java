package me.itzgeoff.vidsync.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Retention(RetentionPolicy.RUNTIME)
@Component
@Profile("server")
public @interface ServerComponent {

}
