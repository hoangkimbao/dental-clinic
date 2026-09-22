package com.dentalclinic.repository;
import com.dentalclinic.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop20ByOrderByCreatedAtDesc();
    List<Notification> findByTargetRoleOrTargetRoleOrderByCreatedAtDesc(String targetRole, String allRole);
}
