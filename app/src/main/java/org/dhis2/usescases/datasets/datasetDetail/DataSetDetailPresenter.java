package org.dhis2.usescases.datasets.datasetDetail;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.IntDef;

import org.dhis2.data.metadata.MetadataRepository;
import org.dhis2.usescases.datasets.dataSetTable.DataSetTableActivity;
import org.dhis2.usescases.datasets.dataSetTable.DataSetTableModel;
import org.dhis2.usescases.datasets.datasetInitial.DataSetInitialActivity;
import org.dhis2.utils.Constants;
import org.dhis2.utils.OrgUnitUtils;
import org.dhis2.utils.Period;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.period.PeriodType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;



public class DataSetDetailPresenter implements DataSetDetailContract.Presenter {

    private DataSetDetailRepository dataSetDetailRepository;
    private DataSetDetailContract.View view;
    private CategoryOptionComboModel categoryOptionComboModel;
    private MetadataRepository metadataRepository;
    private int lastSearchType;
    private Date fromDate;
    private Date toDate;
    private Period period;
    private List<Date> dates;
    private CompositeDisposable compositeDisposable;
    private List<OrganisationUnitModel> orgUnits;
    private CategoryComboModel mCatCombo;
    private List<String> selectedOrgUnits;
    private PeriodType selectedPeriodType;
    private Map<String, String> mapPeriodAvailable;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LastSearchType.DATES, LastSearchType.DATE_RANGES})
    public @interface LastSearchType {
        int DATES = 1;
        int DATE_RANGES = 32;
    }

    public DataSetDetailPresenter(DataSetDetailRepository dataSetDetailRepository, MetadataRepository metadataRepository) {
        this.dataSetDetailRepository = dataSetDetailRepository;
        this.metadataRepository = metadataRepository;
        compositeDisposable = new CompositeDisposable();
        mapPeriodAvailable = new HashMap<>();
    }

    @Override
    public void init(DataSetDetailContract.View view) {
        this.view = view;
        getOrgUnits(null);
        compositeDisposable.add(
                view.dataSetPage()
                        .startWith(0)
                        .flatMap(page -> dataSetDetailRepository.dataSetGroups(view.dataSetUid(), selectedOrgUnits, selectedPeriodType, page))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                dataSetDetailModels -> {
                                    for(DataSetDetailModel dataset: dataSetDetailModels)
                                        mapPeriodAvailable.put(dataset.periodId(), dataset.namePeriod());

                                    view.setData(dataSetDetailModels);
                                    view.setWritePermission(view.accessDataWrite());
                                },
                                Timber::d
                        )
        );
    }

    @Override
    public void onTimeButtonClick() {
        view.showTimeUnitPicker();
    }

    @Override
    public void onDateRangeButtonClick() {
        view.showRageDatePicker();
    }

    @Override
    public void onOrgUnitButtonClick() {
        view.openDrawer();
    }


    @Override
    public void addDataSet() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DATA_SET_UID, view.dataSetUid());

        view.startActivity(DataSetInitialActivity.class,bundle,false,false,null);
    }

    @Override
    public void onBackClick() {
        if (view != null)
            view.back();
    }

    @Override
    public void onCatComboSelected(CategoryOptionComboModel categoryOptionComboModel, String
            orgUnitQuery) {
        //updateFilters(categoryOptionComboModel, orgUnitQuery);
    }

    @Override
    public void clearCatComboFilters(String orgUnitQuery) {
        //updateFilters(null, orgUnitQuery);
    }

    @Override
    public void onDataSetClick(String orgUnit, String orgUnitName, String periodId, String periodType, String initPeriodType, String catOptionComb) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ORG_UNIT, orgUnit);
        bundle.putString(Constants.ORG_UNIT_NAME, orgUnitName);
        bundle.putString(Constants.PERIOD_TYPE_DATE, initPeriodType);
        bundle.putString(Constants.PERIOD_TYPE, periodType);
        bundle.putString(Constants.PERIOD_ID, periodId);
        bundle.putString(Constants.CAT_COMB, catOptionComb);
        bundle.putString(Constants.DATA_SET_UID, view.dataSetUid());
        bundle.putBoolean(Constants.ACCESS_DATA, view.accessDataWrite());
        bundle.putString(Constants.PERIOD_ID, periodId);
        view.startActivity(DataSetTableActivity.class, bundle, false, false, null);
    }

    @Override
    public List<OrganisationUnitModel> getOrgUnits() {
        return this.orgUnits;
    }

    @Override
    public void showFilter() {
        view.showHideFilter();
    }

    @SuppressLint("CheckResult")
    @Override
    public void getDataSets(Date fromDate, Date toDate, String orgUnitQuery) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        lastSearchType = LastSearchType.DATES;
        /*Observable.just(dataSetDetailRepository.filteredDataSet(programId,
                DateUtils.getInstance().formatDate(fromDate),
                DateUtils.getInstance().formatDate(toDate),
                categoryOptionComboModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list ->view.setData(getPeriodFromType(list)),
                        Timber::e));*/
    }

    @Override
    public void getOrgUnits(Date date) {
        compositeDisposable.add(dataSetDetailRepository.orgUnits()
                .map(orgUnits -> {
                    this.orgUnits = orgUnits;
                    return OrgUnitUtils.renderTree(view.getContext(), orgUnits, true);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        treeNode -> view.addTree(treeNode),
                        throwable -> view.renderError(throwable.getMessage())
                ));
    }

    private void updateFilters(CategoryOptionComboModel categoryOptionComboModel, String
            orgUnitQuery) {
        this.categoryOptionComboModel = categoryOptionComboModel;
        switch (lastSearchType) {
            case LastSearchType.DATES:
                getDataSets(this.fromDate, this.toDate, orgUnitQuery);
                break;
            case LastSearchType.DATE_RANGES:
                //getDataSetWithDates(this.dates, this.period, orgUnitQuery);
                break;
            default:
                //getDataSetWithDates(null, this.period, orgUnitQuery);
                break;
        }
    }

    @Override
    public void getDataSetWithDates(List<String> dates, Period period, List<String> orgUnitQuery) {
        this.period = period;
        lastSearchType = LastSearchType.DATE_RANGES;
        //ahora falla por que se va a hacer la select y no puede
        /*compositeDisposable.add(dataSetDetailRepository.filteredDataSet(programId,"","", categoryOptionComboModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list ->view.setData(getPeriodFromType(list)),
                        throwable -> view.renderError(throwable.getMessage())));*/
    }

    @Override
    public void onDettach() {
        compositeDisposable.clear();
    }

    @Override
    public void displayMessage(String message) {
        view.displayMessage(message);
    }

    @Override
    public Map<String, String> getPeriodAvailableForFilter() {
        return mapPeriodAvailable;
    }
}
