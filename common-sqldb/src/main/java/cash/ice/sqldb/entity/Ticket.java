package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Data
@Accessors(chain = true)
public class Ticket implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "body")
    private String body;

    @Column(name = "staff_user_id")
    private Integer staffUserId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
