// Draws a slowly moving network of glowing dots and lines behind the page.
// It matches the theme: a network being watched.
(function () {
    const canvas = document.getElementById('network-bg');
    if (!canvas) return;   // page has no background canvas, do nothing

    const ctx = canvas.getContext('2d');
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const LINK_DISTANCE = 140;        // dots closer than this get joined by a line
    const MOUSE_DISTANCE = 170;       // dots near the cursor connect to it
    const DOT_COLOR = '56, 189, 248'; // cyan (red, green, blue)

    let width = 0;
    let height = 0;
    let dots = [];
    const mouse = { x: null, y: null };

    function resize() {
        const ratio = window.devicePixelRatio || 1;   // keeps it sharp on high-resolution screens
        width = window.innerWidth;
        height = window.innerHeight;
        canvas.width = width * ratio;
        canvas.height = height * ratio;
        canvas.style.width = width + 'px';
        canvas.style.height = height + 'px';
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);

        // More screen space means more dots, capped so it stays smooth
        const count = Math.min(90, Math.floor((width * height) / 18000));
        dots = Array.from({ length: count }, () => ({
            x: Math.random() * width,
            y: Math.random() * height,
            vx: (Math.random() - 0.5) * 0.4,
            vy: (Math.random() - 0.5) * 0.4,
            size: Math.random() * 1.5 + 1
        }));
    }

    function moveDots() {
        for (const dot of dots) {
            dot.x += dot.vx;
            dot.y += dot.vy;
            if (dot.x < 0 || dot.x > width)  dot.vx *= -1;   // bounce off the edges
            if (dot.y < 0 || dot.y > height) dot.vy *= -1;
        }
    }

    function drawLine(x1, y1, x2, y2, alpha) {
        ctx.strokeStyle = `rgba(${DOT_COLOR}, ${alpha})`;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();
    }

    function draw() {
        ctx.clearRect(0, 0, width, height);

        if (!reduceMotion) moveDots();

        // Dots
        for (const dot of dots) {
            ctx.beginPath();
            ctx.arc(dot.x, dot.y, dot.size, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(${DOT_COLOR}, 0.85)`;
            ctx.fill();
        }

        // Lines between nearby dots (closer = brighter)
        for (let i = 0; i < dots.length; i++) {
            for (let j = i + 1; j < dots.length; j++) {
                const distance = Math.hypot(dots[i].x - dots[j].x, dots[i].y - dots[j].y);
                if (distance < LINK_DISTANCE) {
                    drawLine(dots[i].x, dots[i].y, dots[j].x, dots[j].y,
                             (1 - distance / LINK_DISTANCE) * 0.35);
                }
            }
            // Lines from dots to the mouse pointer
            if (mouse.x !== null) {
                const distance = Math.hypot(dots[i].x - mouse.x, dots[i].y - mouse.y);
                if (distance < MOUSE_DISTANCE) {
                    drawLine(dots[i].x, dots[i].y, mouse.x, mouse.y,
                             (1 - distance / MOUSE_DISTANCE) * 0.6);
                }
            }
        }

        if (!reduceMotion) requestAnimationFrame(draw);
    }

    window.addEventListener('resize', () => { resize(); if (reduceMotion) draw(); });
    window.addEventListener('mousemove', e => { mouse.x = e.clientX; mouse.y = e.clientY; });
    window.addEventListener('mouseout', () => { mouse.x = null; mouse.y = null; });

    resize();
    draw();
})();