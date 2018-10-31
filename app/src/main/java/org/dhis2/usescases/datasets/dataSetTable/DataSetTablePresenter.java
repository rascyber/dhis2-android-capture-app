package org.dhis2.usescases.datasets.dataSetTable;

import android.support.annotation.NonNull;

import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.dhis2.usescases.datasets.dataSetTable.dataSetSection.DataSetSectionFragment;
import org.hisp.dhis.android.core.category.CategoryModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.dataelement.DataElementModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DataSetTablePresenter implements DataSetTableContract.Presenter {

    private final DataSetTableRepository tableRepository;
    DataSetTableContract.View view;
    private CompositeDisposable compositeDisposable;
    private Pair<Map<String, List<DataElementModel>>, Map<String, List<List<Pair<CategoryOptionModel, CategoryModel>>>>> tableData;
    private List<DataSetTableModel> dataValues;
    private String orgUnitUid;
    private String periodTypeName;
    private String periodInitialDate;
    private String catCombo;

    public DataSetTablePresenter(DataSetTableRepository dataSetTableRepository) {
        this.tableRepository = dataSetTableRepository;
    }

    @Override
    public void onBackClick() {
        view.back();
    }

    @Override
    public void init(DataSetTableContract.View view, String orgUnitUid, String periodTypeName, String periodInitialDate, String catCombo) {
        this.view = view;
        compositeDisposable = new CompositeDisposable();
        this.orgUnitUid = orgUnitUid;
        this.periodTypeName = periodTypeName;
        this.periodInitialDate = periodInitialDate;
        this.catCombo = catCombo;


        compositeDisposable.add(
                Flowable.zip(
                        tableRepository.getDataElements(),
                        tableRepository.getCatOptions(),
                        Pair::create
                )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    this.tableData = data;
                                    view.setDataElements(data.val0(), data.val1());
                                },
                                Timber::e
                        )
        );
    }

    @Override
    public void getData(@NonNull DataSetSectionFragment dataSetSectionFragment, @Nullable String sectionUid) {
        compositeDisposable.add(
                Flowable.zip(
                        tableRepository.getDataValues(orgUnitUid, periodTypeName, periodInitialDate, catCombo),
                        tableRepository.getDataSet(),
                        tableRepository.getCatOptionCombo(),
                        Trio::create
                )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                trio -> {
                                    view.setDataValue(trio.val0());
                                    view.setDataSet(trio.val1());

                                    dataSetSectionFragment.setData(tableData.val0(), transformCategories(tableData.val1()), trio.val0(), trio.val2());
                                },
                                Timber::e
                        )
        );
    }

    @Override
    public Map<String, List<List<CategoryOptionModel>>> transformCategories(@NonNull Map<String, List<List<Pair<CategoryOptionModel, CategoryModel>>>> map) {
        Map<String, List<List<CategoryOptionModel>>> mapTransform = new HashMap<>();
        for( Map.Entry<String, List<List<Pair<CategoryOptionModel, CategoryModel>>>> entry: map.entrySet()){
            mapTransform.put(entry.getKey(), new ArrayList<>());
            int repeat = 0;
            for(List<Pair<CategoryOptionModel, CategoryModel>> list: map.get(entry.getKey())){
                repeat++;
                List<CategoryOptionModel> catOptions = new ArrayList<>();
                for(int x = 0; x<repeat; x++) {
                    for(Pair<CategoryOptionModel, CategoryModel> pair: list){
                        catOptions.add(pair.val0());
                    }
                }
                mapTransform.get(entry.getKey()).add(catOptions);
            }


        }
        return mapTransform;
    }


    @Override
    public void onDettach() {
        compositeDisposable.dispose();
    }

    @Override
    public void displayMessage(String message) {
        view.displayMessage(message);
    }


}