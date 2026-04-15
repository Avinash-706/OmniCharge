package com.omnicharge.notification.repository;

import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testFindByUserId() {
        // Create test notifications
        Notification notification1 = createNotification(100L, "user1@example.com", NotificationCategory.PAYMENT_SUCCESS);
        Notification notification2 = createNotification(100L, "user1@example.com", NotificationCategory.PLAN_EXPIRED);
        Notification notification3 = createNotification(200L, "user2@example.com", NotificationCategory.PAYMENT_FAILED);

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        // Test findByUserId
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> result = notificationRepository.findByUserId(100L, pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(n -> n.getUserId().equals(100L)));
    }

    @Test
    void testCountByUserIdAndIsRead() {
        // Create test notifications
        Notification notification1 = createNotification(100L, "user1@example.com", NotificationCategory.PAYMENT_SUCCESS);
        notification1.setIsRead(false);
        
        Notification notification2 = createNotification(100L, "user1@example.com", NotificationCategory.PLAN_EXPIRED);
        notification2.setIsRead(false);
        
        Notification notification3 = createNotification(100L, "user1@example.com", NotificationCategory.PAYMENT_FAILED);
        notification3.setIsRead(true);

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        // Test countByUserIdAndIsRead
        long unreadCount = notificationRepository.countByUserIdAndIsRead(100L, false);
        long readCount = notificationRepository.countByUserIdAndIsRead(100L, true);

        assertEquals(2, unreadCount);
        assertEquals(1, readCount);
    }

    @Test
    void testSaveAndFindById() {
        Notification notification = createNotification(100L, "user@example.com", NotificationCategory.PAYMENT_SUCCESS);
        Notification saved = notificationRepository.save(notification);

        assertNotNull(saved.getId());
        
        Notification found = notificationRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(100L, found.getUserId());
    }

    @Test
    void testDeleteNotification() {
        Notification notification = createNotification(100L, "user@example.com", NotificationCategory.PAYMENT_SUCCESS);
        Notification saved = notificationRepository.save(notification);

        notificationRepository.deleteById(saved.getId());

        assertFalse(notificationRepository.findById(saved.getId()).isPresent());
    }

    private Notification createNotification(Long userId, String email, NotificationCategory category) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserEmail(email);
        notification.setUserMobile("+919876543210");
        notification.setType(NotificationType.EMAIL);
        notification.setCategory(category);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId("REF-" + System.currentTimeMillis());
        notification.setIsRead(false);
        return notification;
    }
}
