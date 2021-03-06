package chat.rocket.persistence.realm.repositories;

import android.os.Looper;
import android.support.v4.util.Pair;

import com.hadisatrio.optional.Optional;

import chat.rocket.core.models.PublicSetting;
import chat.rocket.core.repositories.PublicSettingRepository;
import chat.rocket.persistence.realm.RealmStore;
import chat.rocket.persistence.realm.models.ddp.RealmPublicSetting;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.realm.RealmResults;

public class RealmPublicSettingRepository extends RealmRepository
        implements PublicSettingRepository {

    private final String hostname;

    public RealmPublicSettingRepository(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public Single<Optional<PublicSetting>> getById(String id) {
        return Single.defer(() -> Flowable.using(
                () -> new Pair<>(RealmStore.getRealm(hostname), Looper.myLooper()),
                pair -> {
                    if (pair.first == null) {
                        return Flowable.empty();
                    }

                    return pair.first.where(RealmPublicSetting.class)
                            .equalTo(RealmPublicSetting.ID, id)
                            .findAll()
                            .<RealmResults<RealmPublicSetting>>asFlowable();
                },
                pair -> close(pair.first, pair.second)
        )
                .unsubscribeOn(AndroidSchedulers.from(Looper.myLooper()))
                .filter(it -> it.isLoaded() && it.isValid())
                .map(it -> getPublicSettingOrDefault(id, it))
                .first(Optional.absent()));
    }

    private Optional<PublicSetting> getPublicSettingOrDefault(String id, RealmResults<RealmPublicSetting> results) {
        if (results.size() > 0) {
            return Optional.of(results.get(0).asPublicSetting());
        }

        PublicSetting defaultSetting = PublicSetting.builder()
                .setId(id)
                .setValue("")
                .setUpdatedAt(0L)
                .build();
        return Optional.of(defaultSetting);
    }
}
