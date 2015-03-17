(function() {
  $(function() {

    $('#addr_name').keyup(function(e) {
      if (e.keyCode == 13) {
        settings.addAddress();
      }
    });

    $('#update_user').click(function(e) {
      settings.updateUser("#updatesettings_form");
      return false;
    });
    
    $("#addresses").find('.del').click(function(event) {
      var t = $(this).parent();
      var c = t.next();
      t.remove();
      c.remove();
      return false;
    });

    $("#settings_tab").tabify();
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
        timeout : 3000,
        data : $(formId).serialize(),
        timeout : 3000,
        statusCode : {
          201 : function(msg) {
            common.showNotice(msg);
          },
          
          403 : function(msg) {
            var data = JSON.parse(msg.responseText);
            if (data.errors) {
              common.showFormErrors(data.errors);
            }            
          }
        }
      });
    });
  },

  addAddress : function() {
    var name = $('#addr_name').val();

    if (name.trim()) {
      var title = $('.address_template .accordion_title').clone();
      var content = $('.address_template .accordion_content').clone();

      title.find('.addr_title').text(name);
      content.find("label").each(function(e) {
          $(this).attr("for", $(this).attr("for") + name);
      });
      content.find("input").each(function(e) {
        $(this).attr("name", $(this).attr("name") + name);
        $(this).attr("id", $(this).attr("id") + name);
      });

      $('#addresses').append(title).append(content);

      title.find('.del').click(function(event) {
        var t = $(this).parent();
        var c = t.next();
        t.remove();
        c.remove();
        return false;
      });

      $('.accordion > .accordion_title').unbind();
      allPanels = $('.accordion > .accordion_content');
      settings.refreshAccordion();
    }
  },

  refreshAccordion : function() {
    console.log('refreshAccordion');
    $('.accordion > .accordion_title').unbind();
    opened = undefined;
    $('.accordion > .accordion_title').click(function() {
      console.log('refreshAccordion');
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