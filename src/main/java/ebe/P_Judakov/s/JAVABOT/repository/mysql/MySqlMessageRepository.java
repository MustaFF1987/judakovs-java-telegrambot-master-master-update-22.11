package ebe.P_Judakov.s.JAVABOT.repository.mysql;

import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.Chat;
import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.Message;
import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.User;
import ebe.P_Judakov.s.JAVABOT.repository.interfaces.MessageRepository;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public class MySqlMessageRepository implements MessageRepository {

    private MessageRepository messageJpaRepository;

    @Override
    public List<Message> findByUser(User user) {
        return messageJpaRepository.findByUser(user);
    }

    @Override
    public List<Message> findByChat(Chat chat) {
        return messageJpaRepository.findByChat(chat);
    }

    @Override
    public List<Message> findByDateBetween(Date startDate, Date endDate) {
        return messageJpaRepository.findByDateBetween(startDate, endDate);
    }

    @Override
    public Message findTopByChatOrderByDateDesc(Chat chat) {
        return messageJpaRepository.findTopByChatOrderByDateDesc(chat);
    }

    @Override
    public int countByChat(Chat chat) {
        return messageJpaRepository.countByChat(chat);
    }
}
