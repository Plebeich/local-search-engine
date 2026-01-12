package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "`index`")
public class ModelIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private ModelPage page;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private ModelLemma lemma;

    @Column(name = "`rank`",nullable = false)
    private Float rank;
}
