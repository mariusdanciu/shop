(function() {
    $(function() {

        $.blockUI.defaults.baseZ = 90;
        categories.reloadCategories();

    });
})();

var categories = {

    refreshList : function() {

        $(".close_dialog").bind("click", function(event) {
            window.common.closeDialog();
            return false;
        });

        $(".cat_box").each(function(index) {
            var me = $(this);
            var pid = me.attr("id");

            if (window.admin !== undefined) {
                me.find('.edit_tag_close').click(function(event) {
                    window.admin.deleteCategory(pid);
                    return false;
                });

                me.find('.edit_tag_update').click(function(event) {
                    window.admin.editCategory(pid);
                    return false;
                });

            }

            if (pid !== undefined) {
                me.click(function(event) {
                    window.location.href = "/products?cat=" + pid;
                    return false;
                });
            } else {
                if (window.admin !== undefined) {
                    window.admin.attachCreateCategory(me);
                }
            }
        });
    },

    reloadCategories : function() {
        $.ajax({
            url : "/",
            cache : false,
            dataType : "html",
            error : function(xhr) {
                common.showError(xhr.statusText);
            },
            success : function(data) {
                $("#item_list").html(data);

                categories.refreshList();
                $("#item_list").fadeIn({
                    duration : 1000
                });

                return false;
            }
        });

    }
}