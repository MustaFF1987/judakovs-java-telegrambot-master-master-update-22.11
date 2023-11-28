package ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces;

import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.JpaUser;

import java.util.List;

public interface Chat {

        int getId();
        int getChatId();
        List<JpaUser> getUsers();
        void addUser(JpaUser user);
    }

