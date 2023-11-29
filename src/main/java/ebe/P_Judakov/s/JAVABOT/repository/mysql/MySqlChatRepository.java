package ebe.P_Judakov.s.JAVABOT.repository.mysql;

import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.Chat;
import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.Message;
import ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces.User;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.JpaChat;
import ebe.P_Judakov.s.JAVABOT.domen.entity.jpa.SubscribedChannel;
import ebe.P_Judakov.s.JAVABOT.repository.interfaces.ChatRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MySqlChatRepository implements ChatRepository {

    private EntityManager entityManager;

    public MySqlChatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // Реализация метода для поиска чатов по пользователю
    @Override
    public List<Chat> findByUsers(User user) {
        Query query = entityManager.createQuery("SELECT c FROM Chat c WHERE :user MEMBER OF c.users");
        query.setParameter("user", user);
        return query.getResultList();
    }

    // Реализация метода для поиска чата по его идентификатору
    @Override
    public Chat findByChatId(Long chatId) {
        return entityManager.find(Chat.class, chatId);
    }


    // Реализация метода для поиска сообщений по чату
    @Override
    public List<Message> findMessagesByChat(Chat chat) {
         Query query = entityManager.createQuery("SELECT m FROM Message m WHERE m.chat = :chat");
         query.setParameter("chat", chat);
         return query.getResultList();
    }

    // Реализация метода для сохранения чата
    @Override
    public Chat save(Chat chat) {
        entityManager.getTransaction().begin();
        entityManager.persist(chat);
        entityManager.getTransaction().commit();
        return chat;
    }

    // Реализация метода для удаления чата
    @Override
    public void delete(Chat chat) {
        entityManager.getTransaction().begin();
        entityManager.remove(chat);
        entityManager.getTransaction().commit();
    }

    // Реализация метода для подсчета количества пользователей в чате
    @Override
    public int countUsersByChat(Chat chat) {
        if (chat instanceof JpaChat) {
            JpaChat jpaChat = (JpaChat) chat;
            if (jpaChat.getUsers() != null) {
                return jpaChat.getUsers().size();
            }
        }
        return 0;
    }


    // Реализация метода для поиска подписанных каналов по chatId
    @Override
    public List<SubscribedChannel> findByChatId(int chatId) {
        return entityManager.createQuery("SELECT sc FROM SubscribedChannel sc WHERE sc.chatId = :chatId", SubscribedChannel.class)
                .setParameter("chatId", chatId)
                .getResultList();
    }


    // Реализация метода для поиска чата по его идентификатору (JPA)
    @Override
    public Optional<JpaChat> findById(int id) {
        return Optional.ofNullable(entityManager.find(JpaChat.class, id));
    }
}
