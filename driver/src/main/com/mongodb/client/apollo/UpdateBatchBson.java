package com.mongodb.client.apollo;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.operation.UpdateOperation;
import org.bson.conversions.Bson;

/**
 * 批量修改元素
 * Created with IntelliJ IDEA.
 * Class: UpdateBatch
 * User: likang
 * Date: 2017/3/29
 * Time: 18:43
 * To change this template use File | Settings | File Templates.
 */
public class UpdateBatchBson {
    private Bson filter;
    private Bson update;
    private UpdateOptions updateOptions;

    public UpdateBatchBson() {
    }

    public UpdateBatchBson(Bson filter, Bson update,UpdateOptions updateOptions) {
        this.filter = filter;
        this.update = update;
        this.updateOptions = updateOptions;
    }

    public Bson getFilter() {
        return filter;
    }

    public UpdateBatchBson setFilter(Bson filter) {
        this.filter = filter;
        return this;
    }

    public Bson getUpdate() {
        return update;
    }

    public UpdateBatchBson setUpdate(Bson update) {
        this.update = update;
        return this;
    }

    public UpdateOptions getUpdateOptions() {
        return updateOptions;
    }

    public UpdateBatchBson setUpdateOptions(UpdateOptions updateOptions) {
        this.updateOptions = updateOptions;
        return this;
    }
}
