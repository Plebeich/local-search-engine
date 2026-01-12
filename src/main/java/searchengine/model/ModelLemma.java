package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class ModelLemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private ModelSite site;

    @Column(columnDefinition = "VARCHAR(255)",nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;
}
