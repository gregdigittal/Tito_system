package cash.ice.zim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Entity
@Table(name = "Wallets")
@Data
@Accessors(chain = true)
public class LegacyWallets implements Serializable {

    @Id
    @Column(name = "Wallet_ID")
    private Integer walletId;

    @Column(name = "Wallet_Name")
    private String walletName;

    @Column(name = "Currency")
    private String currency;

    @Column(name = "Description")
    private String description;

    @Column(name = "Wallet_Type")
    private String walletType;

    @Column(name = "ISO_Currency_Code")
    private String isoCurrencyCode;
}
