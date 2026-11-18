package volodea.fruitful.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

@Entity
@Table(name = "conversations")
@Builder
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Column(name = "user_a_last_read_at")
    private OffsetDateTime userALastReadAt;

    @Column(name = "user_b_last_read_at")
    private OffsetDateTime userBLastReadAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) createdAt = now;
        if (userALastReadAt == null) userALastReadAt = now;
        if (userBLastReadAt == null) userBLastReadAt = now;
    }
}
