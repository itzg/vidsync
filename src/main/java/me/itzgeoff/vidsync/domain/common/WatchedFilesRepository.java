package me.itzgeoff.vidsync.domain.common;


import org.springframework.data.repository.CrudRepository;

public interface WatchedFilesRepository extends CrudRepository<WatchedFile, Long> {
	WatchedFile findByPath(String pathToFile);
	
	WatchedFile findByContentSignature(String signature);
}
