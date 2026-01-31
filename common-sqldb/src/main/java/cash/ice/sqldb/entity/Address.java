package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "address")
@Data
@Accessors(chain = true)
public class Address implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "city")
    private String city;

    @Column(name = "address_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "notes")
    private String notes;
}
