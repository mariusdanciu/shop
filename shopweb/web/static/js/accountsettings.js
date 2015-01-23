(function() {
  $(function() {

    $('#addr_name').keyup(function(e) {
      if (e.keyCode == 13) {
        settings.addAddress();
      }
    });

    $('#update_user').click(function(e) {
      settings.updateUser("#updatesettings_form");
      e.stopPropagation();
      e.preventDefault();
    });

    settings.refreshAccordion();
  })
})();

var opened = undefined;

var settings = {

  updateUser : function(formId) {
    $(formId).each(function() {
      var frm = this;

      $(formId + ' label').css("color", "#555555").removeAttr("title");
      $.ajax({
        url : $(formId).attr('action'),
        type : "POST",
        cache : false,
        data : $(formId).serialize(),
        statusCode : {
          201 : function() {
          }
        }
      }).fail(function(msg, f) {
        $("#notice_connect_e").html(msg.responseText);
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      });
    });
  },

  addAddress : function() {
    var name = $('#addr_name').val();

    if (name.trim()) {
      var title = $('.address_template .accordion_title').clone();
      var content = $('.address_template .accordion_content').clone();

      title.find('.addr_title').text(name);
      content.find("input").each(function(e) {
        $(this).attr("name", $(this).attr("name") + "/" + name);
        $(this).attr("id", $(this).attr("id") + "/" + name);
      });

      $('#addresses').append(title).append(content);

      title.find('.del').click(function(event) {
        var t = $(this).parent();
        var c = t.next();
        t.remove();
        c.remove();
        event.stopPropagation();
        event.preventDefault();
      });

      $('.accordion > .accordion_title').unbind();
      allPanels = $('.accordion > .accordion_content');
      settings.refreshAccordion();
    }
  },

  refreshAccordion : function() {
    $('.accordion > .accordion_title').click(function() {
      var allPanels = $('.accordion > .accordion_content');
      allPanels.slideUp();
      if (opened !== this) {
        $(this).next().slideDown();
        opened = this;
      } else {
        opened = undefined;
      }
      return false;
    });
  }

}