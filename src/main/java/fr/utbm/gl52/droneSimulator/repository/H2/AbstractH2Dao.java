package fr.utbm.gl52.droneSimulator.repository.H2;

import fr.utbm.gl52.droneSimulator.repository.AbstractDao;
import fr.utbm.gl52.droneSimulator.repository.HibernateHelper;
import fr.utbm.gl52.droneSimulator.service.entity.DbDrone;
import fr.utbm.gl52.droneSimulator.service.entity.DbParameter;
import fr.utbm.gl52.droneSimulator.service.entity.DbParcel;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * @param <E> MyEntity
 */
public abstract class AbstractH2Dao<E> extends AbstractDao<E> {

    AbstractH2Dao(Class<E> clazz) {
        super(clazz);
    }

    public E get(long id) {
        Session session = HibernateHelper.getSessionFactory().openSession();

        E entity = session.get(clazz, id);

        session.close();
        return entity;
    }

    public void delete(E entity) {
        Session session = HibernateHelper.getSessionFactory().openSession();
        session.beginTransaction();

        session.delete(entity);

        session.getTransaction().commit();
        session.close();
    }

    public List<E> list() {
        Session session = HibernateHelper.getSessionFactory().openSession();

        List entities = session.createQuery("FROM " + clazz.getSimpleName()).list();

        session.close();

        return entities;
    }

    public List<E> list(int simulationId) {
        Session session = HibernateHelper.getSessionFactory().openSession();

        List entities = session.createQuery("FROM " + clazz.getSimpleName() + " WHERE idSimu = " + simulationId).list();

        session.close();

        return entities;
    }

    public List<DbParcel> list(int simulationId, String event) {
        Session session = HibernateHelper.getSessionFactory().openSession();

        List entities = session.createQuery("FROM " + clazz.getSimpleName() + " WHERE idsimu = " + simulationId + " AND event = '" + event+"'").list();

        session.close();

        return entities;
    }

    public DbParameter parameterSimu(int simulationId) {
        Session session = HibernateHelper.getSessionFactory().openSession();

        List<DbParameter> entities = session.createQuery("FROM " + clazz.getSimpleName() + " WHERE idsimu = " + simulationId).list();

        session.close();

        return entities.get(0);
    }

    public int nbEventIterationSimu(int simulationId, int iterationId, String event) {
        Session session = HibernateHelper.getSessionFactory().openSession();

        List entities = session.createQuery("FROM " + clazz.getSimpleName() + " WHERE idsimu = " + simulationId + " AND iditeration = " + iterationId + " AND event = '" + event + "'").list();

        session.close();

        return entities.size();
    }

    public void save(E entity) {
        Session session = HibernateHelper.getSessionFactory().openSession();
        session.beginTransaction();

        session.save(entity);

        session.getTransaction().commit();
        session.close();
    }

    public void merge(E entity) {
        Session session = HibernateHelper.getSessionFactory().openSession();
        session.beginTransaction();

        session.merge(entity);

        session.getTransaction().commit();
        session.close();
    }

    public List<DbDrone> droneListInFirstIteration(int idSimu){
        Session session = HibernateHelper.getSessionFactory().openSession();

        List<DbDrone> entities = session.createQuery("FROM " + clazz.getSimpleName() + " WHERE idSimu = " + idSimu + " AND IDITERATION = 1").list();

        session.close();
        return entities;
    }

    public DbDrone getDroneBy(Integer idSimu, Integer currentIteration, Integer droneId){
        Session session = HibernateHelper.getSessionFactory().openSession();

        Query query = session.createQuery("from DbDrone where idDrone = :idDrone and idIteration = :idIteration and idSimu = :idSimu");
        query.setParameter("idDrone", droneId);
        query.setParameter("idIteration", currentIteration);
        query.setParameter("idSimu", idSimu);
        List<DbDrone> dbdrones = query.list();

        session.close();
        return dbdrones.get(0);
    }

}