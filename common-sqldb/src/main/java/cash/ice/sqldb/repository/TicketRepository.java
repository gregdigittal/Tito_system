package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
}