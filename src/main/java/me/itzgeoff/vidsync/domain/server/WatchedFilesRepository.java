package me.itzgeoff.vidsync.domain.server;

import org.springframework.data.repository.CrudRepository;

public interface WatchedFilesRepository extends CrudRepository<WatchedFile, Long> {
	WatchedFile findByPath(String pathToFile);
}
