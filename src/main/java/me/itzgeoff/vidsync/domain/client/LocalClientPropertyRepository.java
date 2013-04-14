package me.itzgeoff.vidsync.domain.client;

import org.springframework.data.repository.CrudRepository;

public interface LocalClientPropertyRepository extends CrudRepository<LocalClientProperty, String> {

    LocalClientProperty findByKey(String key);
}
