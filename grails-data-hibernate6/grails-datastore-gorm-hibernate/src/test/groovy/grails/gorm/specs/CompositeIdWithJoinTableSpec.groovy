package grails.gorm.specs

import grails.gorm.annotation.Entity
import org.jetbrains.annotations.NotNull


import static grails.gorm.hibernate.mapping.MappingBuilder.define

/**
 * Created by graemerocher on 26/01/2017.
 */
//TODO: Failing at MappingModelCreationHelper line 1223
//MappingModelCreationHelper assert ( (SortableValue) collectionBootValueMapping.getKey() ).isSorted()
class CompositeIdWithJoinTableSpec extends HibernateGormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [CompositeIdParent,CompositeIdChild]
    }

    //    @Rollback
    void "test composite id with join table"() {
        when:"A parent with a composite id and a join table is saved"
        new CompositeIdParent(name: "Test" , last:"Test 2")
                .addToChildren(new CompositeIdChild(foo: "bar"))
                .save(flush:true)


        then:"The entity was saved"
        CompositeIdParent.count() == 1
        CompositeIdParent.list().first().children.size() == 1
    }
}

@Entity
class CompositeIdParent implements Serializable ,  Comparable<CompositeIdParent>{
    String name
    String last
    SortedSet<CompositeIdChild> children
    static hasMany = [children:CompositeIdChild]
    static mapping = define {
        id composite('name','last')
        property("children") {
            joinTable {
                name "child_parent"
                column "child_id"
            }
            column {
                name "foo"
            }
            column {
                name "bar"
            }
        }
    }

    @Override
    int compareTo(@NotNull CompositeIdParent o) {
        this.name <=> o.name ?: this.last <=> o.last
    }
}

@Entity
class CompositeIdChild implements Comparable<CompositeIdChild> {
    String foo
    static belongsTo = [parent:CompositeIdParent]

    static mapping = {

    }
    static constraints = {
    }
    @Override
    int compareTo(CompositeIdChild other) {
        foo <=> other.foo
    }
}