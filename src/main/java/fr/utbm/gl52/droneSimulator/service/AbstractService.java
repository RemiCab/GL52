package fr.utbm.gl52.droneSimulator.service;

import fr.utbm.gl52.droneSimulator.repository.AbstractDao;
import fr.utbm.gl52.droneSimulator.repository.H2.AbstractH2Dao;
import fr.utbm.gl52.droneSimulator.service.entity.DbDrone;
import fr.utbm.gl52.droneSimulator.service.entity.DbParameter;
import fr.utbm.gl52.droneSimulator.service.entity.DbParcel;
import fr.utbm.gl52.droneSimulator.service.entity.MyEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Use different Dao to persist entities
 * @param <E> MyEntity
 */
public abstract class AbstractService<E extends MyEntity> implements ServiceInterface{
    protected AbstractDao<E> mySqlDao;
    protected AbstractDao<E> redisDao;
    protected Class<E> clazz;

    public AbstractService(Class<E> clazz) {
        this.clazz = clazz;
    }

    public AbstractH2Dao<E> getH2Dao() {
        if (mySqlDao == null){
            mySqlDao = getDao("H2");
        }
        return (AbstractH2Dao<E>) mySqlDao;
    }

    public AbstractDao<E> getDao(String technology) {
        Object dao = null;
        try {
            System.out.println("fr.utbm.gl52.droneSimulator.repository.H2." + technology + clazz.getSimpleName() + "Dao");
            Class daoClass = Class.forName("fr.utbm.gl52.droneSimulator.repository.H2." + technology + clazz.getSimpleName() + "Dao");
            dao = daoClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return (AbstractDao<E>) dao;
    }

    public E get(int id) {
        return getInDatabase(id);
    }

    public E getInDatabase(int id) {
        return getH2Dao().get(id);
    }

    public void delete(int id) {
        E entity = getInDatabase(id);
        deleteInDatabase(entity);
    }

    public void deleteInDatabase(E entity) {
        getH2Dao().delete(entity);
    }

    public void save(E entity) {
        saveInDatabase(entity);
    }

    public void saveInDatabase(E entity) {
        getH2Dao().save(entity);
    }

    public void merge(E entity){
        getH2Dao().merge(entity);
    }

    public List<E> getAllFromSimulationId(int simulationId) {
        return getH2Dao().list(simulationId);
    }

    public DbParameter getSimulationParameter(int simulationId) {
        return getH2Dao().parameterSimu(simulationId);
    }

    public List<E> getAllSimulation() {
        return getH2Dao().list();
    }

    public int getNbEventIterationSimu(int simulationId, int iterationId, String event) {
        return getH2Dao().nbEventIterationSimu(simulationId,iterationId,event);
    }
}
