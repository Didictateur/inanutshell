package fr.didictateur.inanutshell;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile FolderDao _folderDao;

  private volatile RecetteDao _recetteDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Folder` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `parentId` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `Recette` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `titre` TEXT, `taille` TEXT, `tempsPrep` TEXT, `ingredients` TEXT, `preparation` TEXT, `notes` TEXT, `imageResId` INTEGER NOT NULL, `parentId` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c9981d8ce603696e88e62ea43be0cc8d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `Folder`");
        db.execSQL("DROP TABLE IF EXISTS `Recette`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsFolder = new HashMap<String, TableInfo.Column>(3);
        _columnsFolder.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolder.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolder.put("parentId", new TableInfo.Column("parentId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFolder = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFolder = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFolder = new TableInfo("Folder", _columnsFolder, _foreignKeysFolder, _indicesFolder);
        final TableInfo _existingFolder = TableInfo.read(db, "Folder");
        if (!_infoFolder.equals(_existingFolder)) {
          return new RoomOpenHelper.ValidationResult(false, "Folder(fr.didictateur.inanutshell.Folder).\n"
                  + " Expected:\n" + _infoFolder + "\n"
                  + " Found:\n" + _existingFolder);
        }
        final HashMap<String, TableInfo.Column> _columnsRecette = new HashMap<String, TableInfo.Column>(9);
        _columnsRecette.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("titre", new TableInfo.Column("titre", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("taille", new TableInfo.Column("taille", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("tempsPrep", new TableInfo.Column("tempsPrep", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("ingredients", new TableInfo.Column("ingredients", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("preparation", new TableInfo.Column("preparation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("imageResId", new TableInfo.Column("imageResId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecette.put("parentId", new TableInfo.Column("parentId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRecette = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRecette = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRecette = new TableInfo("Recette", _columnsRecette, _foreignKeysRecette, _indicesRecette);
        final TableInfo _existingRecette = TableInfo.read(db, "Recette");
        if (!_infoRecette.equals(_existingRecette)) {
          return new RoomOpenHelper.ValidationResult(false, "Recette(fr.didictateur.inanutshell.Recette).\n"
                  + " Expected:\n" + _infoRecette + "\n"
                  + " Found:\n" + _existingRecette);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "c9981d8ce603696e88e62ea43be0cc8d", "7337e4f0851a0854f171f8afe2e70f9b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "Folder","Recette");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `Folder`");
      _db.execSQL("DELETE FROM `Recette`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(FolderDao.class, FolderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RecetteDao.class, RecetteDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public FolderDao folderDao() {
    if (_folderDao != null) {
      return _folderDao;
    } else {
      synchronized(this) {
        if(_folderDao == null) {
          _folderDao = new FolderDao_Impl(this);
        }
        return _folderDao;
      }
    }
  }

  @Override
  public RecetteDao recetteDao() {
    if (_recetteDao != null) {
      return _recetteDao;
    } else {
      synchronized(this) {
        if(_recetteDao == null) {
          _recetteDao = new RecetteDao_Impl(this);
        }
        return _recetteDao;
      }
    }
  }
}
