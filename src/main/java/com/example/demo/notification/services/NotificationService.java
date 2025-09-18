package com.example.demo.notification.services;

import com.example.demo.auth_users.entity.User;
import com.example.demo.notification.dtos.NotificationDTO;

public interface NotificationService {

    void sendEmail(NotificationDTO notificationDTO, User user);

}
