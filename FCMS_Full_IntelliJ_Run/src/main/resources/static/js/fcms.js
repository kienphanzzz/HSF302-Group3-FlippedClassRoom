document.addEventListener('DOMContentLoaded', function () {
  if (window.lucide) window.lucide.createIcons();

  document.querySelectorAll('[data-copy]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      const value = btn.getAttribute('data-copy') || '';
      navigator.clipboard && navigator.clipboard.writeText(value);
      const old = btn.innerHTML;
      btn.innerHTML = '<i data-lucide="check"></i>';
      if (window.lucide) window.lucide.createIcons();
      setTimeout(function(){ btn.innerHTML = old; if (window.lucide) window.lucide.createIcons(); }, 1400);
    });
  });

  document.querySelectorAll('[data-toggle-topic]').forEach(function (head) {
    head.addEventListener('click', function () {
      const target = document.getElementById(head.getAttribute('data-toggle-topic'));
      if (target) target.classList.toggle('hidden');
    });
  });

  const fileInput = document.querySelector('#submissionFile');
  const fileList = document.querySelector('#fileList');
  if (fileInput && fileList) {
    fileInput.addEventListener('change', function () {
      fileList.innerHTML = '';
      Array.from(fileInput.files || []).forEach(function (file) {
        const row = document.createElement('div');
        row.className = 'file-row';
        row.innerHTML = '<i data-lucide="paperclip" class="text-primary"></i><div><div class="small" style="font-weight:750">' + file.name + '</div><div class="small muted">' + Math.round(file.size/1024) + ' KB</div></div>';
        fileList.appendChild(row);
      });
      if (window.lucide) window.lucide.createIcons();
    });
  }
});

// Auth page helpers
(function () {
  document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      const id = btn.getAttribute('data-toggle-password');
      const input = document.getElementById(id);
      if (!input) return;
      input.type = input.type === 'password' ? 'text' : 'password';
    });
  });
})();
