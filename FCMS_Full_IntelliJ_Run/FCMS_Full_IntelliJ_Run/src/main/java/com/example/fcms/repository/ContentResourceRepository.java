package com.example.fcms.repository;

import com.example.fcms.entity.ContentResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentResourceRepository extends JpaRepository<ContentResource, Long> {
    List<ContentResource> findByLearningNode_NodeIdAndVisibleTrue(Long nodeId);
}
