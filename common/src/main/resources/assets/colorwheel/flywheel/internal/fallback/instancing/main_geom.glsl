void main()
{
    // Do the average as a best effort attempt
    clrwl_overlayColor = (clrwl_in[0].clrwl_overlayColor + clrwl_in[1].clrwl_overlayColor + clrwl_in[2].clrwl_overlayColor) / 3;
    clrwl_out.clrwl_overlayColor = clrwl_overlayColor;

    _clrwl_shader_main();
}
